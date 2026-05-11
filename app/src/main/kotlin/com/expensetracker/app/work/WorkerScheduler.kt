package com.expensetracker.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    fun scheduleAll(context: Context) {
        scheduleInactivityReminder(context)
        scheduleQuoteOfTheDay(context)
        scheduleBudgetAlert(context)
    }

    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(AppActivityWorker.WORK_NAME)
        workManager.cancelUniqueWork(QuoteOfTheDayWorker.WORK_NAME)
        workManager.cancelUniqueWork(BudgetAlertWorker.WORK_NAME)
    }

    fun scheduleInactivityReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val request = PeriodicWorkRequestBuilder<AppActivityWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AppActivityWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleQuoteOfTheDay(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val initialDelay = calculateInitialDelayFor9AM()

        val request = PeriodicWorkRequestBuilder<QuoteOfTheDayWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            QuoteOfTheDayWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleBudgetAlert(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BudgetAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun sendInactivityReminderNow(context: Context) {
        val inputData = workDataOf(AppActivityWorker.KEY_DEBUG_FORCE to true)
        val request = OneTimeWorkRequestBuilder<AppActivityWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "debug_inactivity_reminder",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun sendQuoteNow(context: Context) {
        val inputData = workDataOf(QuoteOfTheDayWorker.KEY_DEBUG_FORCE to true)
        val request = OneTimeWorkRequestBuilder<QuoteOfTheDayWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "debug_quote_of_the_day",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun sendDailyBudgetAlertNow(context: Context) {
        val inputData = workDataOf(BudgetAlertWorker.KEY_DEBUG_DAILY to true)
        val request = OneTimeWorkRequestBuilder<BudgetAlertWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "debug_daily_alert",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun sendWeeklyBudgetAlertNow(context: Context) {
        val inputData = workDataOf(BudgetAlertWorker.KEY_DEBUG_WEEKLY to true)
        val request = OneTimeWorkRequestBuilder<BudgetAlertWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "debug_weekly_alert",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun sendMonthlyBudgetAlertNow(context: Context) {
        val inputData = workDataOf(BudgetAlertWorker.KEY_DEBUG_MONTHLY to true)
        val request = OneTimeWorkRequestBuilder<BudgetAlertWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "debug_monthly_alert",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun calculateInitialDelayFor9AM(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
