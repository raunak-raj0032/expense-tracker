package com.expensetracker.app.ui.screens.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LedgerUiState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val selectedTypes: Set<TransactionType> = emptySet(),
    val selectedAccountIds: Set<Long> = emptySet(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val startDate = state.startDate ?: LocalDate.now().minusMonths(1)
            val endDate = state.endDate ?: LocalDate.now()

            val transactions = transactionRepository.getForDateRange(startDate, endDate)
                .filter { tx ->
                    (state.selectedTypes.isEmpty() || state.selectedTypes.contains(tx.type)) &&
                    (state.searchQuery.isEmpty() || 
                        tx.description?.contains(state.searchQuery, ignoreCase = true) == true ||
                        tx.notes?.contains(state.searchQuery, ignoreCase = true) == true)
                }
                .sortedByDescending { it.transactionTime }

            _uiState.update {
                it.copy(
                    transactions = transactions,
                    isLoading = false
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadTransactions()
    }

    fun updateSelectedTypes(types: Set<TransactionType>) {
        _uiState.update { it.copy(selectedTypes = types) }
    }

    fun updateSelectedAccounts(accountIds: Set<Long>) {
        _uiState.update { it.copy(selectedAccountIds = accountIds) }
    }
    
    fun setDateRange(start: LocalDate?, end: LocalDate?) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
    }

    fun applyFilters() {
        loadTransactions()
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedTypes = emptySet(),
                selectedAccountIds = emptySet(),
                startDate = null,
                endDate = null
            )
        }
        loadTransactions()
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.delete(id)
            loadTransactions()
        }
    }
}