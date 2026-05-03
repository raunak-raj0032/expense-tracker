package com.expensetracker.app.ui.screens.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.AccountRepository
import com.expensetracker.app.core.data.repository.TagRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.dao.TransactionTagDao
import com.expensetracker.app.core.model.Account
import com.expensetracker.app.core.model.Tag
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject

enum class DateRangePreset {
    ALL, THIS_MONTH, LAST_MONTH, LAST_90_DAYS, YEAR_TO_DATE, CUSTOM
}

data class LedgerUiState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val selectedTypes: Set<TransactionType> = emptySet(),
    val selectedAccountIds: Set<Long> = emptySet(),
    val selectedTagIds: Set<Long> = emptySet(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val datePreset: DateRangePreset = DateRangePreset.ALL,
    val minAmount: String = "",
    val maxAmount: String = "",
    val accounts: List<Account> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTransactionIds: Set<Long> = emptySet()
) {
    val selectionMode: Boolean get() = selectedTransactionIds.isNotEmpty()
    val activeFilterCount: Int
        get() = listOf(
            selectedTypes.isNotEmpty(),
            selectedAccountIds.isNotEmpty(),
            selectedTagIds.isNotEmpty(),
            datePreset != DateRangePreset.ALL,
            minAmount.isNotBlank() || maxAmount.isNotBlank()
        ).count { it }
}

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val tagRepository: TagRepository,
    private val transactionTagDao: TransactionTagDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var tagsByTransaction: Map<Long, Set<Long>> = emptyMap()

    init {
        observeLookups()
        observeTransactions()
    }

    private fun observeLookups() {
        viewModelScope.launch {
            accountRepository.observeActive().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        viewModelScope.launch {
            tagRepository.observeAll().collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            transactionRepository.observeAll().collect { transactions ->
                allTransactions = transactions
                tagsByTransaction = transactions.associate { tx ->
                    tx.id to transactionTagDao.getTagIdsForTransaction(tx.id).toSet()
                }
                applyFiltersInternal()
            }
        }
    }

    private fun applyFiltersInternal() {
        val state = _uiState.value
        val minMinor = state.minAmount.toAmountMinor()
        val maxMinor = state.maxAmount.toAmountMinor()
        val startMillis = state.startDate?.atStartOfDay(java.time.ZoneId.systemDefault())
            ?.toInstant()?.toEpochMilli()
        val endMillis = state.endDate?.plusDays(1)
            ?.atStartOfDay(java.time.ZoneId.systemDefault())
            ?.toInstant()?.toEpochMilli()

        val filtered = allTransactions.filter { tx ->
            val txMillis = tx.transactionTime.atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val txTags = tagsByTransaction[tx.id].orEmpty()

            (state.selectedTypes.isEmpty() || tx.type in state.selectedTypes) &&
                (state.selectedAccountIds.isEmpty() ||
                    tx.accountId in state.selectedAccountIds ||
                    (tx.counterpartyAccountId != null && tx.counterpartyAccountId in state.selectedAccountIds)) &&
                (state.selectedTagIds.isEmpty() ||
                    txTags.any { it in state.selectedTagIds }) &&
                (startMillis == null || txMillis >= startMillis) &&
                (endMillis == null || txMillis < endMillis) &&
                (minMinor == null || tx.amountMinor >= minMinor) &&
                (maxMinor == null || tx.amountMinor <= maxMinor) &&
                (state.searchQuery.isBlank() || tx.matchesQuery(state.searchQuery))
        }.sortedByDescending { it.transactionTime }

        _uiState.update { it.copy(transactions = filtered, isLoading = false) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersInternal()
    }

    fun updateSelectedTypes(types: Set<TransactionType>) {
        _uiState.update { it.copy(selectedTypes = types) }
    }

    fun updateSelectedAccounts(accountIds: Set<Long>) {
        _uiState.update { it.copy(selectedAccountIds = accountIds) }
    }

    fun updateSelectedTags(tagIds: Set<Long>) {
        _uiState.update { it.copy(selectedTagIds = tagIds) }
    }

    fun updateMinAmount(value: String) {
        if (value.isEmpty() || value.isValidAmount()) {
            _uiState.update { it.copy(minAmount = value) }
        }
    }

    fun updateMaxAmount(value: String) {
        if (value.isEmpty() || value.isValidAmount()) {
            _uiState.update { it.copy(maxAmount = value) }
        }
    }

    fun setDatePreset(preset: DateRangePreset) {
        val today = LocalDate.now()
        val (start, end) = when (preset) {
            DateRangePreset.ALL -> null to null
            DateRangePreset.THIS_MONTH -> today.withDayOfMonth(1) to today
            DateRangePreset.LAST_MONTH -> {
                val firstOfThis = today.withDayOfMonth(1)
                val firstOfLast = firstOfThis.minusMonths(1)
                firstOfLast to firstOfThis.minusDays(1)
            }
            DateRangePreset.LAST_90_DAYS -> today.minusDays(89) to today
            DateRangePreset.YEAR_TO_DATE -> today.withDayOfYear(1) to today
            DateRangePreset.CUSTOM -> _uiState.value.startDate to _uiState.value.endDate
        }
        _uiState.update { it.copy(datePreset = preset, startDate = start, endDate = end) }
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        _uiState.update {
            it.copy(datePreset = DateRangePreset.CUSTOM, startDate = start, endDate = end)
        }
    }

    fun applyFilters() {
        applyFiltersInternal()
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedTypes = emptySet(),
                selectedAccountIds = emptySet(),
                selectedTagIds = emptySet(),
                startDate = null,
                endDate = null,
                datePreset = DateRangePreset.ALL,
                minAmount = "",
                maxAmount = ""
            )
        }
        applyFiltersInternal()
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.delete(id)
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update {
            val updated = if (id in it.selectedTransactionIds)
                it.selectedTransactionIds - id
            else
                it.selectedTransactionIds + id
            it.copy(selectedTransactionIds = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedTransactionIds = emptySet()) }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedTransactionIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            transactionRepository.deleteAll(ids)
            _uiState.update { it.copy(selectedTransactionIds = emptySet()) }
        }
    }

    private fun Transaction.matchesQuery(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (description?.contains(q, ignoreCase = true) == true) return true
        if (notes?.contains(q, ignoreCase = true) == true) return true
        val state = _uiState.value
        val accName = state.accounts.firstOrNull { it.id == accountId }?.name
        if (accName?.contains(q, ignoreCase = true) == true) return true
        return false
    }

    private fun String.isValidAmount(): Boolean {
        if (all { it.isDigit() || it == '.' }.not()) return false
        val parts = split(".")
        return parts.size <= 2 && (parts.size != 2 || parts[1].length <= 2)
    }

    private fun String.toAmountMinor(): Long? {
        if (isBlank()) return null
        val bd = toBigDecimalOrNull() ?: return null
        return bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}
