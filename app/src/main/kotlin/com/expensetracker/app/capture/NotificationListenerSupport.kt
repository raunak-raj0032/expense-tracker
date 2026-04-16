package com.expensetracker.app.capture

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService

fun requestCaptureNotificationRebind(context: Context) {
    NotificationListenerService.requestRebind(
        ComponentName(context, ExpenseNotificationListenerService::class.java)
    )
}
