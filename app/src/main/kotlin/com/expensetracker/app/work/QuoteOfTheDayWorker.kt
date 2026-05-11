package com.expensetracker.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expensetracker.app.auth.AuthRepository
import com.expensetracker.app.auth.AuthState
import com.expensetracker.app.budget.BudgetPeriod
import com.expensetracker.app.budget.BudgetTracker
import com.expensetracker.app.core.notif.LastActiveTracker
import com.expensetracker.app.core.notif.NotificationChannels
import com.expensetracker.app.core.notif.NotificationHelper
import com.expensetracker.app.ui.mascot.PiggyMood
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class QuoteOfTheDayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val lastActiveTracker: LastActiveTracker,
    private val authRepository: AuthRepository,
    private val budgetTracker: BudgetTracker
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "quote_of_the_day_worker"
        const val NOTIFICATION_ID = 4911
        const val KEY_DEBUG_FORCE = "debug_force"
    }

    private val quotes = listOf(
        "Small daily savings lead to big financial freedom.",
        "Track every rupee, waste not a single one.",
        "Wealth is not about how much you earn — it's how much you keep.",
        "Your future self will thank you for every ₹100 saved today.",
        "Financial freedom is freedom from worry.",
        "Every expense you track is a step toward your goals.",
        "A penny saved is a penny earned.",
        "Budgeting isn't restriction — it's making your money work for you.",
        "Today's discipline, tomorrow's freedom.",
        "The best time to save was last month. The second best is now.",
        "Track the small things; the big picture takes care of itself.",
        "You can't improve what you don't measure.",
        "Consistency beats perfection — keep logging.",
        "Money saved is money earned — and you just earned today!",
        "Your piggy bank is getting heavier!",
        "Savings grow quietly. Keep going.",
        "Every ₹100 you save is a vote for your future.",
        "Slow and steady wins the money race.",
        "The habit of saving is the root of all wealth.",
        "Know where your money goes, and watch it grow.",
        "Discipline today, freedom tomorrow.",
        "One rupee saved is one rupee earned.",
        "Don't count your money while sitting at the table.",
        "Track it, control it, grow it."
    )

    override suspend fun doWork(): Result {
        val forceSend = inputData.getBoolean(KEY_DEBUG_FORCE, false)
        if (!forceSend && lastActiveTracker.isTodayQuoteSent()) {
            return Result.success()
        }

        val quote = quotes.random()
        val firstName = getFirstName()
        val savingsAmount = getMonthlySavings()

        val title = "Your daily money wisdom 💡"
        val body = buildString {
            append("\"$quote\"")
            if (savingsAmount != null && firstName.isNotBlank()) {
                append(" — You've saved $savingsAmount this month!")
            }
        }

        NotificationHelper.showNotification(
            context = applicationContext,
            notificationId = NOTIFICATION_ID,
            channelId = NotificationChannels.SMART_REMINDERS,
            title = title,
            body = body,
            mood = PiggyMood.Cheer,
            navRoute = "home"
        )

        if (!forceSend) {
            lastActiveTracker.markQuoteSentToday()
        }

        return Result.success()
    }

    private suspend fun getFirstName(): String {
        val authState = authRepository.authState.first()
        return (authState as? AuthState.SignedIn)?.user?.firstName.orEmpty()
    }

    private suspend fun getMonthlySavings(): String? {
        return try {
            val snapshot = budgetTracker.observe(BudgetPeriod.MONTHLY).first()
            if (snapshot.hasBudget && snapshot.remainingMinor != null && snapshot.remainingMinor > 0) {
                val formatted = com.expensetracker.app.core.money.CurrencyConverter.formatAmount(snapshot.remainingMinor)
                formatted
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
