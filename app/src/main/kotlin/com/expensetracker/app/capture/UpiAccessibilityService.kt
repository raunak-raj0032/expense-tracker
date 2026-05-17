package com.expensetracker.app.capture

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.expensetracker.app.R
import com.expensetracker.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class UpiAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var captureEventRepository: CaptureEventRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Dedupe: keyed by fingerprint of payload text.
    private var lastFingerprint: String? = null
    private var lastFingerprintAt: Long = 0L

    // Rate-limit CONTENT_CHANGED events per package (package → last-read timestamp).
    private val lastContentReadAt = mutableMapOf<String, Long>()

    // Pending delayed reads per package so we cancel/replace on rapid events.
    private val pendingReads = mutableMapOf<String, Job>()

    companion object {
        private const val TAG = "UpiCapture"
        private const val CHANNEL_ID = "upi_capture_confirm"
        private const val KEEPALIVE_CHANNEL_ID = "upi_capture_keepalive"
        private const val KEEPALIVE_NOTIFICATION_ID = 4242
        private const val DEDUPE_WINDOW_MS = 60_000L
        private const val MAX_TRAVERSAL_NODES = 800
        // Delay after window-state-changed so animations finish before we read the tree.
        private const val WINDOW_SETTLE_MS = 500L
        // Minimum gap between content-changed reads for the same package.
        private const val CONTENT_CHANGE_THROTTLE_MS = 2_000L

        private val SUPPORTED_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.google.android.apps.nbu.paisa.provider",
            "com.phonepe.app",
            "com.phonepe.app.preprod",
            "net.one97.paytm",
            "com.paytm.app",
            "in.org.npci.bhimapp",
            "in.amazon.mShop.android.shopping",
            "com.dreamplug.androidapp",
            "com.mobikwik_new",
            "com.freecharge.android",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.upi.axispay",
            "com.sbi.upi",
            "com.fss.unbipsp",
            "com.csam.icici.bank.imobile",
            "com.axisbank.digibank",
            "com.icici.bank.imobile"
        )

        private val SUCCESS_KEYWORDS = listOf(
            "payment successful",
            "paid successfully",
            "sent successfully",
            "transaction successful",
            "money sent",
            "payment complete",
            "payment completed",
            "payment sent",
            "successfully paid",
            "transfer successful",
            "transfer completed",
            "received successfully",
            "money received",
            "amount debited",
            "amount credited",
            "paid •",
            "paid ·",
            "debited from",
            "credited to",
            "utr",
            "upi transaction id",
            "transaction id",
            "you paid",
            "you sent",
            "₹"        // fallback — any screen with a rupee symbol from a UPI app
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ensureChannel()
        startKeepAliveForeground()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in SUPPORTED_PACKAGES) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // New screen opened — wait for it to finish rendering, then read.
                scheduleDelayedRead(pkg, WINDOW_SETTLE_MS)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Fires constantly while a screen is live. Throttle aggressively.
                val now = System.currentTimeMillis()
                val last = lastContentReadAt[pkg] ?: 0L
                if (now - last >= CONTENT_CHANGE_THROTTLE_MS) {
                    lastContentReadAt[pkg] = now
                    scheduleDelayedRead(pkg, 0L)
                }
            }
            else -> return
        }
    }

    private fun scheduleDelayedRead(pkg: String, delayMs: Long) {
        // Cancel any pending read for this package — we only want the latest one.
        pendingReads[pkg]?.cancel()
        pendingReads[pkg] = serviceScope.launch {
            if (delayMs > 0) delay(delayMs)
            processPackageScreen(pkg)
        }
    }

    private suspend fun processPackageScreen(pkg: String) {
        val text = readTextFromAllWindows(pkg)
        if (text.isNullOrBlank()) {
            Log.v(TAG, "no text collected for $pkg")
            return
        }

        val lower = text.lowercase(Locale.ROOT)
        if (SUCCESS_KEYWORDS.none(lower::contains)) {
            Log.v(TAG, "no success keyword in $pkg (${text.take(120)})")
            return
        }
        Log.d(TAG, "match pkg=$pkg text=${text.take(400)}")

        // Dedupe: fingerprint on package + normalized text (strip clock digits).
        val fp = (pkg + "|" + lower
            .replace(Regex("\\d{1,2}:\\d{2}(:\\d{2})?"), "")
            .replace(Regex("\\d{10,}"), "REF"))          // strip long ref numbers that change
            .hashCode().toString()
        val now = System.currentTimeMillis()
        if (fp == lastFingerprint && now - lastFingerprintAt < DEDUPE_WINDOW_MS) return
        lastFingerprint = fp
        lastFingerprintAt = now

        runCatching {
            val outcome = captureEventRepository.captureAccessibility(
                packageName = pkg,
                text = text
            )
            if (outcome.eventId != null && !outcome.autoImported) {
                postConfirmNotification(outcome.eventId, pkg)
            }
        }.onFailure { Log.e(TAG, "capture failed for $pkg", it) }
    }

    /**
     * Reads text from every window belonging to [targetPkg].
     *
     * Using `windows` (the full window list) is more reliable than
     * `rootInActiveWindow` alone because UPI apps often show their
     * success screen in a dialog or overlay window that isn't the active root.
     */
    private fun readTextFromAllWindows(targetPkg: String): String? {
        val roots = mutableListOf<AccessibilityNodeInfo>()

        runCatching {
            windows
                ?.filter { w -> w.root?.packageName?.toString() == targetPkg }
                ?.mapNotNull { it.root }
                ?.let(roots::addAll)
        }

        // Fallback to rootInActiveWindow if the window list gave us nothing.
        if (roots.isEmpty()) {
            rootInActiveWindow
                ?.takeIf { it.packageName?.toString() == targetPkg }
                ?.let(roots::add)
        }

        if (roots.isEmpty()) return null

        val combined = roots
            .mapNotNull { root ->
                try {
                    collectText(root)
                } finally {
                    root.recycle()
                }
            }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

        return combined.takeIf { it.isNotBlank() }
    }

    /**
     * BFS traversal of [root] collecting text + content descriptions.
     *
     * When a node carries both a text value and a content description that
     * differs from the text, we emit "desc: text" — this reconstructs the
     * label-value pairs common on UPI success screens (e.g. "Amount: ₹500").
     *
     * Nodes are recycled after their children are added to the queue so we
     * don't hold stale references or leak pooled objects on API < 29.
     */
    private fun collectText(root: AccessibilityNodeInfo): String? {
        val pieces = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_TRAVERSAL_NODES) {
            val node = queue.removeFirst()
            visited++

            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()

            val piece = when {
                text.isNotBlank() && desc.isNotBlank() && !desc.equals(text, ignoreCase = true) ->
                    "$desc: $text"
                text.isNotBlank() -> text
                desc.isNotBlank() -> desc
                else -> null
            }
            piece?.let(pieces::add)

            // Enqueue children first, then recycle parent (safe: getChild returns
            // independent objects from the accessibility pool).
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::addLast)
            }
            if (node !== root) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }

        return pieces.distinct().joinToString(" ").trim().takeIf { it.isNotEmpty() }
    }

    override fun onInterrupt() = Unit

    private fun postConfirmNotification(eventId: Long, packageName: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("capture_event_id", eventId)
            putExtra("capture_source", packageName)
        }
        val pi = PendingIntent.getActivity(
            this,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val appLabel = sourceAppName(packageName)
        val accent = ContextCompat.getColor(this, R.color.budget_notification_accent)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Payment ready to review")
            .setContentText("$appLabel capture is waiting in Pocket Pulse")
            .setSmallIcon(R.drawable.ic_notification_pig)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification_pig))
            .setSubText("Pocket Pulse")
            .setColor(accent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Review the $appLabel payment capture and approve it when the amount and merchant look right.")
                    .setBigContentTitle("Payment ready to review")
                    .setSummaryText("Pocket Pulse")
            )
            .setContentIntent(pi)
            .build()
        nm.notify(eventId.toInt(), notification)
    }

    private fun ensureChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel(
                CHANNEL_ID,
                "UPI confirmation prompts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prompts you to confirm UPI payments detected from your UPI apps"
            }.also(nm::createNotificationChannel)
        }
        if (nm.getNotificationChannel(KEEPALIVE_CHANNEL_ID) == null) {
            NotificationChannel(
                KEEPALIVE_CHANNEL_ID,
                "UPI capture running",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps UPI app capture running in the background"
                setShowBadge(false)
            }.also(nm::createNotificationChannel)
        }
    }

    private fun startKeepAliveForeground() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, KEEPALIVE_CHANNEL_ID)
            .setContentTitle("UPI capture is on")
            .setContentText("Watching payment screens quietly in the background")
            .setSmallIcon(R.drawable.ic_notification_pig)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification_pig))
            .setSubText("Pocket Pulse")
            .setColor(ContextCompat.getColor(this, R.color.budget_notification_accent))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pi)
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    KEEPALIVE_NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(KEEPALIVE_NOTIFICATION_ID, notification)
            }
        }.onFailure { Log.w(TAG, "startForeground failed", it) }
    }

    private fun sourceAppName(pkg: String): String = when (pkg) {
        "com.google.android.apps.nbu.paisa.user",
        "com.google.android.apps.nbu.paisa.provider" -> "Google Pay"
        "com.phonepe.app", "com.phonepe.app.preprod" -> "PhonePe"
        "net.one97.paytm", "com.paytm.app" -> "Paytm"
        "in.org.npci.bhimapp" -> "BHIM"
        "in.amazon.mShop.android.shopping" -> "Amazon Pay"
        "com.dreamplug.androidapp" -> "CRED"
        "com.mobikwik_new" -> "MobiKwik"
        "com.freecharge.android" -> "Freecharge"
        "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
        else -> "UPI"
    }
}
