package com.expensetracker.app.core.notif

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.expensetracker.app.R
import com.expensetracker.app.ui.MainActivity
import com.expensetracker.app.ui.mascot.PiggyMood

object NotificationHelper {

    fun showNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        mood: PiggyMood,
        navRoute: String? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val largeIcon = loadHalfCutPenny(context, mood)
        val pendingIntent = createPendingIntent(context, notificationId, navRoute)

        val notification = android.app.Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_pig)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                largeIcon?.let { setLargeIcon(it) }
            }
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun loadHalfCutPenny(context: Context, mood: PiggyMood): Bitmap? = runCatching {
        context.assets.open(halfCutAssetPath(mood)).use { input ->
            BitmapFactory.decodeStream(input)
        }
    }.getOrNull()

    private fun halfCutAssetPath(mood: PiggyMood): String = when (mood) {
        PiggyMood.Happy -> "mascot/emotions/half_cut/penny_cheer.png"
        PiggyMood.Wave -> "mascot/emotions/half_cut/penny_wave.png"
        PiggyMood.Cheer -> "mascot/emotions/half_cut/penny_cheer.png"
        PiggyMood.Wink -> "mascot/emotions/half_cut/penny_wink.png"
        PiggyMood.Sleepy -> "mascot/emotions/half_cut/penny_sleepy.png"
        PiggyMood.Curious -> "mascot/emotions/half_cut/penny_curious.png"
        PiggyMood.Love -> "mascot/emotions/half_cut/penny_love.png"
        PiggyMood.Excited -> "mascot/emotions/half_cut/penny_excited.png"
        PiggyMood.Surprised -> "mascot/emotions/half_cut/penny_surprised.png"
        PiggyMood.Coin -> "mascot/emotions/half_cut/penny_cheer.png"
    }

    private fun createPendingIntent(context: Context, requestCode: Int, navRoute: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            navRoute?.let { putExtra(MainActivity.EXTRA_NAV_ROUTE, it) }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
