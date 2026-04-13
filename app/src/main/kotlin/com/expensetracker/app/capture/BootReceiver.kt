package com.expensetracker.app.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-enable notification listener after boot
            // This helps ensure the service starts automatically
        }
    }
}