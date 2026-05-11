package com.expensetracker.app.core.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val SMART_REMINDERS = "smart_reminders"
    const val BUDGET_ALERTS = "budget_alerts"

    fun create(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val smartRemindersChannel = NotificationChannel(
            SMART_REMINDERS,
            "Smart reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Inactivity reminders and daily motivation"
        }

        val budgetAlertsChannel = NotificationChannel(
            BUDGET_ALERTS,
            "Budget alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when your budget runs low"
        }

        notificationManager.createNotificationChannels(
            listOf(smartRemindersChannel, budgetAlertsChannel)
        )
    }
}