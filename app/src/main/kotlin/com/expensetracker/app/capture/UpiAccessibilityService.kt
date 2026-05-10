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
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class UpiAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var captureEventRepository: CaptureEventRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // In-memory dedupe so we don't reprocess every content-change tick on the
    // same success screen. Keyed by a lightweight hash of the payload text.
    private var lastFingerprint: String? = null
    private var lastFingerprintAt: Long = 0L

    companion object {
        private const val CHANNEL_ID = "upi_capture_confirm"
        private const val KEEPALIVE_CHANNEL_ID = "upi_capture_keepalive"
        private const val KEEPALIVE_NOTIFICATION_ID = 4242
        private const val DEDUPE_WINDOW_MS = 60_000L
        private const val MAX_TRAVERSAL_NODES = 700

        private val SUPPORTED_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user",     // Google Pay (current)
            "com.google.android.apps.nbu.paisa.provider", // Google Pay (legacy)
            "com.phonepe.app",                            // PhonePe
            "com.phonepe.app.preprod",
            "net.one97.paytm",                            // Paytm
            "com.paytm.app",
            "in.org.npci.bhimapp",                        // BHIM
            "in.amazon.mShop.android.shopping",           // Amazon Pay
            "com.dreamplug.androidapp",                   // CRED
            "com.mobikwik_new",                           // MobiKwik
            "com.freecharge.android",                     // Freecharge
            "com.whatsapp",                               // WhatsApp Pay
            "com.whatsapp.w4b",
            "com.upi.axispay",                            // Axis Pay
            "com.sbi.upi",                                // BHIM SBI Pay
            "com.fss.unbipsp",                            // BHIM BOI UPI
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
            "paid •",   // GPay post-payment "Paid • HH:MM" badge
            "paid ·",   // middle-dot variant
            "debited from",
            "credited to",
            "utr",
            "upi transaction id"
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

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val root = rootInActiveWindow ?: run {
            Log.d("UpiCapture", "no root window for $pkg")
            return
        }
        val collected = collectText(root, event) ?: return
        if (collected.isBlank()) return

        val lower = collected.lowercase(Locale.ROOT)
        if (SUCCESS_KEYWORDS.none(lower::contains)) {
            Log.v("UpiCapture", "no success keyword in $pkg screen: ${collected.take(200)}")
            return
        }
        Log.d("UpiCapture", "match pkg=$pkg text=${collected.take(400)}")

        // Debounce repeats of the same screen text.
        val fp = (pkg + "|" + collected.lowercase(Locale.ROOT).replace(Regex("\\d{1,2}:\\d{2}"), "")).hashCode().toString()
        val now = System.currentTimeMillis()
        if (fp == lastFingerprint && now - lastFingerprintAt < DEDUPE_WINDOW_MS) return
        lastFingerprint = fp
        lastFingerprintAt = now

        serviceScope.launch {
            runCatching {
                val outcome = captureEventRepository.captureAccessibility(
                    packageName = pkg,
                    text = collected
                )
                if (outcome.eventId != null && !outcome.autoImported) {
                    postConfirmNotification(outcome.eventId, pkg)
                }
            }
        }
    }

    override fun onInterrupt() {
        // No-op — nothing to cancel on screen.
    }

    private fun collectText(root: AccessibilityNodeInfo, event: AccessibilityEvent): String? {
        val pieces = mutableListOf<String>()
        event.text
            .mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
            .let(pieces::addAll)
        event.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(pieces::add)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_TRAVERSAL_NODES) {
            val node = queue.removeFirst()
            visited += 1
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let(pieces::add)
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(pieces::add)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::addLast)
            }
        }

        return pieces.distinct().joinToString(" ").trim().takeIf { it.isNotEmpty() }
    }

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
            .setSmallIcon(R.drawable.ic_budget_notif)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UPI confirmation prompts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prompts you to confirm UPI payments detected from your UPI apps"
            }
            nm.createNotificationChannel(channel)
        }
        if (nm.getNotificationChannel(KEEPALIVE_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                KEEPALIVE_CHANNEL_ID,
                "UPI capture running",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps UPI app capture running in the background"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun startKeepAliveForeground() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, KEEPALIVE_CHANNEL_ID)
            .setContentTitle("UPI capture is on")
            .setContentText("Watching payment screens quietly in the background")
            .setSmallIcon(R.drawable.ic_budget_notif)
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
        }.onFailure { Log.w("UpiCapture", "startForeground failed", it) }
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
        "com.whatsapp" -> "WhatsApp"
        else -> "UPI"
    }
}
