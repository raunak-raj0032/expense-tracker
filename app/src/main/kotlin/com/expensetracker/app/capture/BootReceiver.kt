package com.expensetracker.app.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.expensetracker.app.budget.BudgetNotificationService
import com.expensetracker.app.core.prefs.UserPreferences
import com.expensetracker.app.work.WorkerScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootEntryPoint {
    fun userPreferences(): UserPreferences
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            requestCaptureNotificationRebind(context)
            WorkerScheduler.scheduleAll(context)
            val pending = goAsync()
            val prefs = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootEntryPoint::class.java
            ).userPreferences()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    if (prefs.budgetNotifEnabled.first()) {
                        BudgetNotificationService.start(context)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
