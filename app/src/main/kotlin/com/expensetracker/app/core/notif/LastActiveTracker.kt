package com.expensetracker.app.core.notif

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastActiveTracker @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "last_active_prefs"
        private const val KEY_LAST_APP_OPEN_MS = "last_app_open_ms"
        private const val KEY_QUOTE_SENT_DATE = "quote_sent_date"
        private const val KEY_DAILY_ALERT_SENT = "daily_alert_sent"
        private const val KEY_WEEKLY_ALERT_SENT = "weekly_alert_sent"
        private const val KEY_MONTHLY_ALERT_SENT = "monthly_alert_sent"
        private const val KEY_LAST_SENT_DATE = "last_sent_date"

        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    var lastAppOpenMs: Long
        get() = prefs.getLong(KEY_LAST_APP_OPEN_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_APP_OPEN_MS, value).apply()

    var quoteSentDate: String
        get() = prefs.getString(KEY_QUOTE_SENT_DATE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_QUOTE_SENT_DATE, value).apply()

    var dailyAlertSent: Boolean
        get() = prefs.getBoolean(KEY_DAILY_ALERT_SENT, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_ALERT_SENT, value).apply()

    var weeklyAlertSent: Boolean
        get() = prefs.getBoolean(KEY_WEEKLY_ALERT_SENT, false)
        set(value) = prefs.edit().putBoolean(KEY_WEEKLY_ALERT_SENT, value).apply()

    var monthlyAlertSent: Boolean
        get() = prefs.getBoolean(KEY_MONTHLY_ALERT_SENT, false)
        set(value) = prefs.edit().putBoolean(KEY_MONTHLY_ALERT_SENT, value).apply()

    var lastSentDate: String
        get() = prefs.getString(KEY_LAST_SENT_DATE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_SENT_DATE, value).apply()

    fun updateLastAppOpen() {
        lastAppOpenMs = System.currentTimeMillis()
    }

    fun isTodayQuoteSent(): Boolean {
        val today = LocalDate.now().format(dateFormatter)
        return quoteSentDate == today
    }

    fun markQuoteSentToday() {
        quoteSentDate = LocalDate.now().format(dateFormatter)
    }

    fun isNewDay(): Boolean {
        val today = LocalDate.now().format(dateFormatter)
        return lastSentDate != today
    }

    fun isNewWeek(): Boolean {
        return !isNewDay() && !weeklyAlertSent
    }

    fun isNewMonth(): Boolean {
        return !isNewDay() && !monthlyAlertSent
    }

    fun resetDailyAlert() {
        dailyAlertSent = false
    }

    fun resetWeeklyAlert() {
        weeklyAlertSent = false
    }

    fun resetMonthlyAlert() {
        monthlyAlertSent = false
    }

    fun resetAllAlertsIfNeeded() {
        if (isNewDay()) {
            lastSentDate = LocalDate.now().format(dateFormatter)
            resetDailyAlert()
        }
    }
}