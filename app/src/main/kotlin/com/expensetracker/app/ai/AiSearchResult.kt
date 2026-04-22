package com.expensetracker.app.ai

data class AiSearchResult(
    val answerText: String,
    val appliedFilters: AiAppliedFilters,
    val matchedTransactionIds: List<Long>,
    val summaryRangeLabel: String?,
    val warningText: String?
)
