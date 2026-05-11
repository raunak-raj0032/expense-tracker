package com.expensetracker.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expensetracker.app.auth.AuthRepository
import com.expensetracker.app.auth.AuthState
import com.expensetracker.app.core.notif.LastActiveTracker
import com.expensetracker.app.core.notif.NotificationChannels
import com.expensetracker.app.core.notif.NotificationHelper
import com.expensetracker.app.ui.mascot.PiggyMood
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AppActivityWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val lastActiveTracker: LastActiveTracker,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "app_activity_worker"
        const val NOTIFICATION_ID = 4910
        const val KEY_DEBUG_FORCE = "debug_force"
        private const val INACTIVITY_THRESHOLD_MS = 4 * 60 * 60 * 1000L // 4 hours
    }

    override suspend fun doWork(): Result {
        val forceSend = inputData.getBoolean(KEY_DEBUG_FORCE, false)
        val lastOpenMs = lastActiveTracker.lastAppOpenMs
        val elapsedMs = System.currentTimeMillis() - lastOpenMs

        if (!forceSend && elapsedMs <= INACTIVITY_THRESHOLD_MS) {
            return Result.success()
        }

        val hoursAway = if (forceSend) 4 else elapsedMs / (60 * 60 * 1000)
        val firstName = getFirstName()

        val title = "Hey, missing you! 👋"
        val body = if (firstName.isNotBlank()) {
            "$firstName, you've been away for $hoursAway hours. Log today's expenses to stay on track!"
        } else {
            "You haven't opened Pocket Pulse in $hoursAway hours. Come log today's expenses!"
        }

        NotificationHelper.showNotification(
            context = applicationContext,
            notificationId = NOTIFICATION_ID,
            channelId = NotificationChannels.SMART_REMINDERS,
            title = title,
            body = body,
            mood = PiggyMood.Curious,
            navRoute = "add_transaction"
        )

        return Result.success()
    }

    private suspend fun getFirstName(): String {
        val authState = authRepository.authState.first()
        return (authState as? AuthState.SignedIn)?.user?.firstName.orEmpty()
    }
}
