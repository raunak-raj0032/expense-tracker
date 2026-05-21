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

        val firstName = getFirstName()

        val title = reminderTitles.random()
        val body = reminderBodies.random().let { message ->
            if (firstName.isNotBlank()) "$firstName, $message" else message.replaceFirstChar { it.uppercase() }
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

    private val reminderTitles = listOf(
        "Tiny money check-in",
        "Penny has a pocket note",
        "Quick expense sparkle",
        "Make future-you smile",
        "Pocket Pulse nudge"
    )

    private val reminderBodies = listOf(
        "ready for a tiny money tidy-up? Add today's spends before they sneak away.",
        "a few taps now can save a lot of guesswork later. Log today's expenses?",
        "your budget buddy is cheering for you. Capture today's little spends.",
        "small habits build calm wallets. Add today's expense trail.",
        "turn today's receipts into clarity. Pocket Pulse is ready when you are.",
        "give your future self a clean ledger. Log today's spends in a minute.",
        "every tracked rupee is a tiny win. Add what you spent today.",
        "quick check-in: did any snacks, rides, or UPI payments need a home?",
        "money mysteries are easier when the clues are fresh. Add today's notes.",
        "keep the streak cozy. A quick log now keeps your budget glowing."
    )
}
