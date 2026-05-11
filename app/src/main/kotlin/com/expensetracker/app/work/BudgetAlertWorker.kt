package com.expensetracker.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expensetracker.app.auth.AuthRepository
import com.expensetracker.app.auth.AuthState
import com.expensetracker.app.budget.BudgetPeriod
import com.expensetracker.app.budget.BudgetTracker
import com.expensetracker.app.core.money.CurrencyConverter
import com.expensetracker.app.core.notif.LastActiveTracker
import com.expensetracker.app.core.notif.NotificationChannels
import com.expensetracker.app.core.notif.NotificationHelper
import com.expensetracker.app.ui.mascot.PiggyMood
import com.expensetracker.app.ui.navigation.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetTracker: BudgetTracker,
    private val lastActiveTracker: LastActiveTracker,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "budget_alert_worker"
        const val DAILY_NOTIFICATION_ID = 4912
        const val WEEKLY_NOTIFICATION_ID = 4913
        const val MONTHLY_NOTIFICATION_ID = 4914

        const val KEY_DEBUG_DAILY = "debug_daily"
        const val KEY_DEBUG_WEEKLY = "debug_weekly"
        const val KEY_DEBUG_MONTHLY = "debug_monthly"
    }

    override suspend fun doWork(): Result {
        val inputData = inputData
        val isDebugDaily = inputData.getBoolean(KEY_DEBUG_DAILY, false)
        val isDebugWeekly = inputData.getBoolean(KEY_DEBUG_WEEKLY, false)
        val isDebugMonthly = inputData.getBoolean(KEY_DEBUG_MONTHLY, false)

        val isDebugMode = isDebugDaily || isDebugWeekly || isDebugMonthly

        if (!isDebugMode) {
            lastActiveTracker.resetAllAlertsIfNeeded()
            checkDailyBudget()
            checkWeeklyBudget()
            checkMonthlyBudget()
        } else {
            if (isDebugDaily) checkDailyBudgetForDebug()
            if (isDebugWeekly) checkWeeklyBudgetForDebug()
            if (isDebugMonthly) checkMonthlyBudgetForDebug()
        }

        return Result.success()
    }

    private suspend fun checkDailyBudget() {
        if (lastActiveTracker.dailyAlertSent) return

        val snapshot = budgetTracker.observe(BudgetPeriod.DAILY).first()
        if (!snapshot.hasBudget) return

        val progressPercent = (snapshot.progressFraction * 100).toInt()

        if (snapshot.progressFraction >= 0.80f) {
            val firstName = getFirstName()
            val remaining = snapshot.remainingMinor ?: 0L
            val remainingFormatted = CurrencyConverter.formatAmount(remaining)

            val title = "Daily budget running low 🔔"
            val body = "$firstName, you've spent $progressPercent% of today's budget. $remainingFormatted left — keep going!"

            NotificationHelper.showNotification(
                context = applicationContext,
                notificationId = DAILY_NOTIFICATION_ID,
                channelId = NotificationChannels.BUDGET_ALERTS,
                title = title,
                body = body,
                mood = PiggyMood.Surprised,
                navRoute = Screen.Budgets.route
            )

            lastActiveTracker.dailyAlertSent = true
        }
    }

    private suspend fun checkWeeklyBudget() {
        if (lastActiveTracker.weeklyAlertSent) return

        val snapshot = budgetTracker.observe(BudgetPeriod.WEEKLY).first()
        if (!snapshot.hasBudget) return

        val progressPercent = (snapshot.progressFraction * 100).toInt()

        if (snapshot.progressFraction >= 0.80f) {
            val firstName = getFirstName()
            val remaining = snapshot.remainingMinor ?: 0L
            val remainingFormatted = CurrencyConverter.formatAmount(remaining)

            val title = "Weekly budget check ⚠️"
            val body = "$firstName, $progressPercent% of this week's budget used. $remainingFormatted remaining. Pace yourself!"

            NotificationHelper.showNotification(
                context = applicationContext,
                notificationId = WEEKLY_NOTIFICATION_ID,
                channelId = NotificationChannels.BUDGET_ALERTS,
                title = title,
                body = body,
                mood = PiggyMood.Excited,
                navRoute = Screen.Budgets.route
            )

            lastActiveTracker.weeklyAlertSent = true
        }
    }

    private suspend fun checkMonthlyBudget() {
        if (lastActiveTracker.monthlyAlertSent) return

        val snapshot = budgetTracker.observe(BudgetPeriod.MONTHLY).first()
        if (!snapshot.hasBudget) return

        val progressPercent = (snapshot.progressFraction * 100).toInt()
        val remaining = snapshot.remainingMinor ?: 0L

        val shouldAlert = snapshot.progressFraction >= 0.95f || remaining < 0

        if (shouldAlert) {
            val firstName = getFirstName()
            val isOverBudget = remaining < 0
            val remainingFormatted = CurrencyConverter.formatAmount(kotlin.math.abs(remaining))

            val title = if (isOverBudget) "Monthly budget at limit 📊" else "Monthly budget almost gone 📊"
            val body = if (isOverBudget) {
                "$firstName, you've overspent your monthly budget by $remainingFormatted. Consider adjusting."
            } else {
                "$firstName, only $remainingFormatted left this month — $progressPercent% used. Last few days, spend wisely!"
            }

            val mood = if (isOverBudget) PiggyMood.Surprised else PiggyMood.Excited

            NotificationHelper.showNotification(
                context = applicationContext,
                notificationId = MONTHLY_NOTIFICATION_ID,
                channelId = NotificationChannels.BUDGET_ALERTS,
                title = title,
                body = body,
                mood = mood,
                navRoute = Screen.Budgets.route
            )

            lastActiveTracker.monthlyAlertSent = true
        }
    }

    private suspend fun getFirstName(): String {
        val authState = authRepository.authState.first()
        return (authState as? AuthState.SignedIn)?.user?.firstName.orEmpty()
    }

    private suspend fun checkDailyBudgetForDebug() {
        val snapshot = budgetTracker.observe(BudgetPeriod.DAILY).first()
        val firstName = getFirstName()

        if (!snapshot.hasBudget) {
            NotificationHelper.showNotification(
                context = applicationContext,
                notificationId = DAILY_NOTIFICATION_ID,
                channelId = NotificationChannels.BUDGET_ALERTS,
                title = "Daily budget running low 🔔",
                body = debugNamePrefix(firstName) + "Daily budget alert test. Set a monthly budget to enable live thresholds.",
                mood = PiggyMood.Surprised,
                navRoute = Screen.Budgets.route
            )
            return
        }

        val progressPercent = (snapshot.progressFraction * 100).toInt()
        val remaining = snapshot.remainingMinor ?: 0L
        val remainingFormatted = CurrencyConverter.formatAmount(remaining)

        val title = "Daily budget running low 🔔"
        val body = if (firstName.isNotBlank()) {
            "$firstName, you've spent $progressPercent% of today's budget. $remainingFormatted left — keep going!"
        } else {
            "You've spent $progressPercent% of today's budget. $remainingFormatted left — keep going!"
        }

        NotificationHelper.showNotification(
            context = applicationContext,
            notificationId = DAILY_NOTIFICATION_ID,
            channelId = NotificationChannels.BUDGET_ALERTS,
            title = title,
            body = body,
            mood = PiggyMood.Surprised,
            navRoute = Screen.Budgets.route
        )
    }

    private suspend fun checkWeeklyBudgetForDebug() {
        val snapshot = budgetTracker.observe(BudgetPeriod.WEEKLY).first()
        val firstName = getFirstName()

        if (!snapshot.hasBudget) {
            NotificationHelper.showNotification(
                context = applicationContext,
                notificationId = WEEKLY_NOTIFICATION_ID,
                channelId = NotificationChannels.BUDGET_ALERTS,
                title = "Weekly budget check ⚠️",
                body = debugNamePrefix(firstName) + "Weekly budget alert test. Set a monthly budget to enable live thresholds.",
                mood = PiggyMood.Excited,
                navRoute = Screen.Budgets.route
            )
            return
        }

        val progressPercent = (snapshot.progressFraction * 100).toInt()
        val remaining = snapshot.remainingMinor ?: 0L
        val remainingFormatted = CurrencyConverter.formatAmount(remaining)

        val title = "Weekly budget check ⚠️"
        val body = if (firstName.isNotBlank()) {
            "$firstName, $progressPercent% of this week's budget used. $remainingFormatted remaining. Pace yourself!"
        } else {
            "$progressPercent% of this week's budget used. $remainingFormatted remaining. Pace yourself!"
        }

        NotificationHelper.showNotification(
            context = applicationContext,
            notificationId = WEEKLY_NOTIFICATION_ID,
            channelId = NotificationChannels.BUDGET_ALERTS,
            title = title,
            body = body,
            mood = PiggyMood.Excited,
            navRoute = Screen.Budgets.route
        )
    }

    private suspend fun checkMonthlyBudgetForDebug() {
        val snapshot = budgetTracker.observe(BudgetPeriod.MONTHLY).first()
        val firstName = getFirstName()

        if (!snapshot.hasBudget) {
            NotificationHelper.showNotification(
                context = applicationContext,
                notificationId = MONTHLY_NOTIFICATION_ID,
                channelId = NotificationChannels.BUDGET_ALERTS,
                title = "Monthly budget alert 📊",
                body = debugNamePrefix(firstName) + "Monthly budget alert test. Set a monthly budget to enable live thresholds.",
                mood = PiggyMood.Excited,
                navRoute = Screen.Budgets.route
            )
            return
        }

        val progressPercent = (snapshot.progressFraction * 100).toInt()
        val remaining = snapshot.remainingMinor ?: 0L
        val isOverBudget = remaining < 0
        val remainingFormatted = CurrencyConverter.formatAmount(kotlin.math.abs(remaining))

        val title = if (isOverBudget) "Monthly budget at limit 📊" else "Monthly budget almost gone 📊"
        val body = if (isOverBudget) {
            if (firstName.isNotBlank()) "$firstName, you've overspent your monthly budget by $remainingFormatted. Consider adjusting."
            else "You've overspent your monthly budget by $remainingFormatted. Consider adjusting."
        } else {
            if (firstName.isNotBlank()) "$firstName, only $remainingFormatted left this month — $progressPercent% used. Last few days, spend wisely!"
            else "Only $remainingFormatted left this month — $progressPercent% used. Last few days, spend wisely!"
        }

        val mood = if (isOverBudget) PiggyMood.Surprised else PiggyMood.Excited

        NotificationHelper.showNotification(
            context = applicationContext,
            notificationId = MONTHLY_NOTIFICATION_ID,
            channelId = NotificationChannels.BUDGET_ALERTS,
            title = title,
            body = body,
            mood = mood,
            navRoute = Screen.Budgets.route
        )
    }

    private fun debugNamePrefix(firstName: String): String =
        if (firstName.isNotBlank()) "$firstName, " else ""
}
