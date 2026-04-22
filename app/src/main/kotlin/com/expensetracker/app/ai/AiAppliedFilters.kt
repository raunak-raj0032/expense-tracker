package com.expensetracker.app.ai

import com.expensetracker.app.core.model.TransactionType
import java.time.LocalDate

/**
 * Structured filter payload the AI returns from an `apply_ledger_filters` tool
 * call. Every field is nullable so the model can express "leave unchanged" by
 * simply omitting it, and the ViewModel maps the set fields onto LedgerUiState.
 */
data class AiAppliedFilters(
    val queryText: String? = null,
    val types: Set<TransactionType>? = null,
    val accountIds: Set<Long>? = null,
    val categoryIds: Set<Long>? = null,
    val tagIds: Set<Long>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null
) {
    val isEmpty: Boolean
        get() = queryText == null &&
            types == null &&
            accountIds == null &&
            categoryIds == null &&
            tagIds == null &&
            startDate == null &&
            endDate == null &&
            minAmountMinor == null &&
            maxAmountMinor == null
}
