@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.expensetracker.app.budget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.expensetracker.app.R
import com.expensetracker.app.core.prefs.UserPreferences
import com.expensetracker.app.ui.MainActivity
import com.expensetracker.app.ui.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BudgetNotificationService : Service() {

    @Inject lateinit var budgetTracker: BudgetTracker
    @Inject lateinit var userPreferences: UserPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observerJob: Job? = null
    private var latestSnapshot: BudgetSnapshot? = null
    private var latestPeriod: BudgetPeriod = BudgetPeriod.MONTHLY

    private val periodCycleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_CYCLE_PERIOD) return
            scope.launch {
                val current = BudgetPeriod.fromName(userPreferences.budgetNotifPeriod.first())
                userPreferences.setBudgetNotifPeriod(current.next().name)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        val filter = IntentFilter(ACTION_CYCLE_PERIOD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(periodCycleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(periodCycleReceiver, filter)
        }

        postForegroundNotification(snapshot = null, period = BudgetPeriod.MONTHLY)

        observerJob = scope.launch {
            userPreferences.budgetNotifPeriod
                .flatMapLatest { periodName ->
                    val period = BudgetPeriod.fromName(periodName)
                    combine(
                        flowOf(period),
                        budgetTracker.observe(period)
                    ) { p, snap -> p to snap }
                }
                .collectLatest { (period, snap) ->
                    latestSnapshot = snap
                    latestPeriod = period
                    postForegroundNotification(snap, period)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REPOST_NOTIFICATION) {
            postForegroundNotification(latestSnapshot, latestPeriod)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observerJob?.cancel()
        scope.cancel()
        runCatching { unregisterReceiver(periodCycleReceiver) }
        super.onDestroy()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget tracker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent ongoing budget summary"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(snapshot: BudgetSnapshot?, period: BudgetPeriod): Notification {
        val openAppPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val addExpensePi = routePendingIntent(Screen.AddTransaction.route, 10)
        val budgetPi    = routePendingIntent(Screen.Budgets.route, 11)
        val cyclePi = PendingIntent.getBroadcast(
            this, 1,
            Intent(ACTION_CYCLE_PERIOD).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val restartPi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this, 3,
                Intent(this, BudgetNotificationService::class.java).setAction(ACTION_REPOST_NOTIFICATION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                this, 3,
                Intent(this, BudgetNotificationService::class.java).setAction(ACTION_REPOST_NOTIFICATION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val titleHtml: String
        val compactHtml: String
        val expandedHtml: String
        val accentColor: Int
        val progressMax: Int
        val progressNow: Int

        val mutedHex = colorHex(R.color.budget_notif_muted)

        if (snapshot == null || !snapshot.hasBudget) {
            accentColor  = ContextCompat.getColor(this, R.color.budget_notification_accent)
            titleHtml    = "<b>Pocket Pulse</b>"
            compactHtml  = "Tap to set a <b>${period.label.lowercase()}</b> budget"
            expandedHtml =
                "No budget set for <b>${period.label}</b>.<br>" +
                "<font color='#$mutedHex'>Tap <b>Budget</b> below to set your spending limit.</font>"
            progressMax  = 0
            progressNow  = 0
        } else {
            val budget    = snapshot.budgetMinor!!
            val spent     = snapshot.spentMinor
            val remaining = snapshot.remainingMinor ?: 0L
            val pct       = (snapshot.progressFraction * 100).toInt().coerceIn(0, 100)

            val statusColorRes = when {
                pct >= 100 -> R.color.budget_notif_status_over
                pct >= 85  -> R.color.budget_notif_status_high
                pct >= 60  -> R.color.budget_notif_status_warn
                else       -> R.color.budget_notif_status_safe
            }
            accentColor = ContextCompat.getColor(this, statusColorRes)
            val accentHex = colorHex(statusColorRes)

            val bar = progressBar(pct)
            val periodContext = snapshot.periodLabel.ifBlank { period.label }

            titleHtml = "<font color='#$accentHex'>●</font>  " +
                "<b>${formatRupees(spent)}</b> " +
                "<font color='#$mutedHex'>spent · ${period.label}</font>"

            compactHtml = if (remaining >= 0)
                "<b>${formatRupees(remaining)}</b> left " +
                    "<font color='#$mutedHex'>·</font> " +
                    "<font color='#$accentHex'>$pct%</font>"
            else
                "<font color='#$accentHex'><b>Over</b></font> by " +
                    "<b>${formatRupees(kotlin.math.abs(remaining))}</b> " +
                    "<font color='#$mutedHex'>·</font> " +
                    "<font color='#$accentHex'>$pct%</font>"

            val summaryLine = if (remaining >= 0)
                "<b>${formatRupees(remaining)}</b> " +
                    "<font color='#$mutedHex'>remaining · $periodContext</font>"
            else
                "<font color='#$accentHex'><b>${formatRupees(kotlin.math.abs(remaining))} over</b></font> " +
                    "<font color='#$mutedHex'>· $periodContext</font>"

            expandedHtml =
                "<font color='#$accentHex'>$bar</font>  <b>$pct%</b><br>" +
                "<font color='#$mutedHex'>${formatRupees(spent)} of</font> " +
                "<b>${formatRupees(budget)}</b><br>" +
                summaryLine

            progressMax = 100
            progressNow = pct
        }

        val titleText    = HtmlCompat.fromHtml(titleHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val compactText  = HtmlCompat.fromHtml(compactHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val expandedText = HtmlCompat.fromHtml(expandedHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget_notif)
            .setContentTitle(titleText)
            .setContentText(compactText)
            .setSubText("Pocket Pulse")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(accentColor)
            .setColorized(false)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setBigContentTitle(titleText)
                    .setSummaryText("Pocket Pulse")
            )
            .setContentIntent(openAppPi)
            .setDeleteIntent(restartPi)
            .apply {
                if (progressMax > 0) setProgress(progressMax, progressNow, false)
                addAction(R.drawable.ic_notif_add,     "Add expense",          addExpensePi)
                addAction(R.drawable.ic_notif_wallet,  "Budget",               budgetPi)
                addAction(R.drawable.ic_notif_refresh, period.next().label,    cyclePi)
            }
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }
    }

    private fun routePendingIntent(route: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAV_ROUTE, route)
        }
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun postForegroundNotification(snapshot: BudgetSnapshot?, period: BudgetPeriod) {
        val notification = buildNotification(snapshot, period)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun formatRupees(minor: Long): String {
        val rupees = minor / 100
        val paise = minor % 100
        val rupeesGrouped = groupIndianDigits(rupees)
        return "₹$rupeesGrouped${if (paise > 0) ".${paise.toString().padStart(2, '0')}" else ""}"
    }

    private fun groupIndianDigits(n: Long): String {
        val s = n.toString()
        if (s.length <= 3) return s
        val head = s.dropLast(3)
        val tail = s.takeLast(3)
        val groupedHead = head.reversed()
            .chunked(2)
            .joinToString(",")
            .reversed()
        return "$groupedHead,$tail"
    }

    private fun progressBar(pct: Int): String {
        val total = 12
        val filled = (pct * total / 100).coerceIn(0, total)
        return "▰".repeat(filled) + "▱".repeat(total - filled)
    }

    private fun colorHex(@androidx.annotation.ColorRes res: Int): String {
        val argb = ContextCompat.getColor(this, res)
        return String.format("%06X", argb and 0xFFFFFF)
    }

    companion object {
        const val CHANNEL_ID = "budget_tracker_persistent"
        const val NOTIFICATION_ID = 4711
        const val ACTION_CYCLE_PERIOD = "com.expensetracker.app.action.CYCLE_BUDGET_PERIOD"
        private const val ACTION_REPOST_NOTIFICATION = "com.expensetracker.app.action.REPOST_BUDGET_NOTIFICATION"

        fun start(context: Context) {
            val intent = Intent(context, BudgetNotificationService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BudgetNotificationService::class.java))
        }
    }
}
