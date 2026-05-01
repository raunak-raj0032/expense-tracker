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
import com.expensetracker.app.R
import com.expensetracker.app.core.prefs.UserPreferences
import com.expensetracker.app.ui.MainActivity
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

        val initialNotif = buildNotification(snapshot = null, period = BudgetPeriod.MONTHLY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                initialNotif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotif)
        }

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
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification(snap, period))
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

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
        val cyclePi = PendingIntent.getBroadcast(
            this, 1,
            Intent(ACTION_CYCLE_PERIOD).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title: String
        val text: String
        val progressMax: Int
        val progressNow: Int
        if (snapshot == null || !snapshot.hasBudget) {
            title = "Budget tracker (${period.label})"
            text = "Set a monthly budget to see ${period.label.lowercase()} pacing"
            progressMax = 0
            progressNow = 0
        } else {
            val budget = snapshot.budgetMinor!!
            val remaining = snapshot.remainingMinor ?: 0L
            val sign = if (remaining >= 0) "" else "-"
            title = "${period.label} budget · ${formatRupees(snapshot.spentMinor)} of ${formatRupees(budget)}"
            text = if (remaining >= 0)
                "$sign${formatRupees(kotlin.math.abs(remaining))} left · ${snapshot.periodLabel}"
            else
                "Over by ${formatRupees(kotlin.math.abs(remaining))} · ${snapshot.periodLabel}"
            progressMax = 100
            progressNow = (snapshot.progressFraction * 100).toInt().coerceIn(0, 100)
        }

        val nextLabel = "Switch to ${period.next().label}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppPi)
            .apply {
                if (progressMax > 0) setProgress(progressMax, progressNow, false)
                addAction(0, nextLabel, cyclePi)
            }
            .build()
    }

    private fun formatRupees(minor: Long): String {
        val rupees = minor / 100
        val paise = minor % 100
        return "₹$rupees${if (paise > 0) ".${paise.toString().padStart(2, '0')}" else ""}"
    }

    companion object {
        const val CHANNEL_ID = "budget_tracker_persistent"
        const val NOTIFICATION_ID = 4711
        const val ACTION_CYCLE_PERIOD = "com.expensetracker.app.action.CYCLE_BUDGET_PERIOD"

        fun start(context: Context) {
            val intent = Intent(context, BudgetNotificationService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BudgetNotificationService::class.java))
        }
    }
}

