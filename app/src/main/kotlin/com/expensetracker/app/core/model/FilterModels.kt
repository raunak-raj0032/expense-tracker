package com.expensetracker.app.core.model

import java.time.LocalDate

data class TransactionFilter(
    val searchQuery: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val categoryIds: List<Long>? = null,
    val accountIds: List<Long>? = null,
    val tagIds: List<Long>? = null,
    val types: List<TransactionType>? = null,
    val statuses: List<TransactionStatus>? = null,
    val sources: List<CaptureSourceType>? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null
)

data class CalendarDay(
    val date: LocalDate,
    val expenseTotal: Long,
    val incomeTotal: Long,
    val netTotal: Long,
    val transactionCount: Int
)

data class CategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val total: Long,
    val percentage: Float
)

data class MerchantBreakdown(
    val merchantId: Long,
    val merchantName: String,
    val total: Long,
    val transactionCount: Int
)