package com.expensetracker.app.core.model

import java.time.LocalDate

data class Budget(
    val id: Long = 0,
    val name: String,
    val periodType: BudgetPeriodType,
    val amountMinor: Long,
    val currencyCode: String = "INR",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val tagId: Long? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isEnabled: Boolean = true
)

enum class BudgetPeriodType {
    WEEKLY,
    MONTHLY,
    CUSTOM
}