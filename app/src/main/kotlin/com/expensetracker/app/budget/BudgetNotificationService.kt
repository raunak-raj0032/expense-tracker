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
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
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
        val openAppPi = openAppPendingIntent(requestCode = 0)
        val addExpensePi = openAppPendingIntent(route = Screen.AddTransaction.route, requestCode = 10)
        val budgetPi = openAppPendingIntent(route = Screen.Budgets.route, requestCode = 11)
        val cyclePi = PendingIntent.getBroadcast(
            this, 1,
            Intent(ACTION_CYCLE_PERIOD).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val restartPi = PendingIntent.getForegroundService(
            this, 3,
            Intent(this, BudgetNotificationService::class.java).setAction(ACTION_REPOST_NOTIFICATION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val titleHtml: String
        val compactHtml: String
        val heroText: String
        val labelText: String
        val metaText: String
        val statusText: String
        val accentColor: Int
        val progressMax: Int
        val progressNow: Int

        val mutedHex = colorHex(R.color.budget_notif_muted)

        if (snapshot == null || !snapshot.hasBudget) {
            accentColor  = ContextCompat.getColor(this, R.color.budget_notification_accent)
            titleHtml    = "<b>Pocket Pulse</b>"
            compactHtml  = "Tap to set a <b>${period.label.lowercase()}</b> budget"
            heroText     = "No budget yet"
            labelText    = "${period.label} plan"
            metaText     = "Set a limit to unlock live spend tracking"
            statusText   = "Setup"
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

            heroText = if (remaining >= 0) formatRupees(remaining) else formatRupees(kotlin.math.abs(remaining))
            labelText = if (remaining >= 0) "left ${period.remainingLabel}" else "over budget"
            metaText = "${formatRupees(spent)} spent of ${formatRupees(budget)}"
            statusText = when {
                pct >= 100 -> "Over"
                pct >= 85  -> "Tight"
                pct >= 60  -> "Watch"
                else       -> "Safe"
            }

            progressMax = 100
            progressNow = pct
        }

        val titleText    = HtmlCompat.fromHtml(titleHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val compactText  = HtmlCompat.fromHtml(compactHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val compactView = budgetRemoteView(
            layoutId = R.layout.notification_budget_compact,
            title = period.label,
            hero = heroText,
            label = labelText,
            meta = metaText,
            status = statusText,
            progressMax = progressMax,
            progressNow = progressNow,
            accentColor = accentColor,
            addExpensePi = addExpensePi,
            budgetPi = budgetPi,
            cyclePi = cyclePi,
            cycleLabel = period.next().label
        )
        val expandedView = budgetRemoteView(
            layoutId = R.layout.notification_budget_expanded,
            title = period.label,
            hero = heroText,
            label = labelText,
            meta = metaText,
            status = statusText,
            progressMax = progressMax,
            progressNow = progressNow,
            accentColor = accentColor,
            addExpensePi = addExpensePi,
            budgetPi = budgetPi,
            cyclePi = cyclePi,
            cycleLabel = period.next().label
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pig)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification_pig))
            .setContentTitle(titleText)
            .setContentText(compactText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setWhen(0L)
            .setUsesChronometer(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(accentColor)
            .setColorized(false)
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setContentIntent(openAppPi)
            .setDeleteIntent(restartPi)
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }
    }

    private fun budgetRemoteView(
        layoutId: Int,
        title: String,
        hero: String,
        label: String,
        meta: String,
        status: String,
        progressMax: Int,
        progressNow: Int,
        accentColor: Int,
        addExpensePi: PendingIntent,
        budgetPi: PendingIntent,
        cyclePi: PendingIntent,
        cycleLabel: String
    ): RemoteViews = RemoteViews(packageName, layoutId).apply {
        setTextViewText(R.id.notif_title, title)
        setTextViewText(R.id.notif_hero, hero)
        setTextViewText(R.id.notif_label, label)
        setTextViewText(R.id.notif_meta, meta)
        setTextViewText(R.id.notif_status, status)
        setTextColor(R.id.notif_status, accentColor)
        setTextColor(R.id.notif_label, accentColor)
        setProgressBar(R.id.notif_progress, progressMax.takeIf { it > 0 } ?: 100, progressNow, progressMax == 0)
        setOnClickPendingIntent(R.id.notif_cta_add, addExpensePi)
        setOnClickPendingIntent(R.id.notif_cta_budget, budgetPi)
        setOnClickPendingIntent(R.id.notif_cta_cycle, cyclePi)
        setTextViewText(R.id.notif_cta_cycle, cycleLabel)
    }

    private fun openAppPendingIntent(route: String? = null, requestCode: Int): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            route?.let { putExtra(MainActivity.EXTRA_NAV_ROUTE, it) }
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

    private fun colorHex(@androidx.annotation.ColorRes res: Int): String {
        val argb = ContextCompat.getColor(this, res)
        return String.format("%06X", argb and 0xFFFFFF)
    }

    private val BudgetPeriod.remainingLabel: String
        get() = when (this) {
            BudgetPeriod.DAILY -> "today"
            BudgetPeriod.WEEKLY -> "this week"
            BudgetPeriod.MONTHLY -> "this month"
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
