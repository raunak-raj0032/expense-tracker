package com.expensetracker.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.CategoryRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.domain.RecurringDetector
import com.expensetracker.app.core.domain.RecurringSeries
import com.expensetracker.app.core.model.CategoryBreakdown
import com.expensetracker.app.core.model.MerchantBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CategoryAnalytics(
    val categoryId: Long,
    val categoryName: String,
    val total: Long,
    val percentage: Float,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
)

data class AnalyticsUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val totalExpense: Long = 0,
    val totalIncome: Long = 0,
    val previousExpense: Long = 0,
    val previousIncome: Long = 0,
    val categoryBreakdown: List<CategoryAnalytics> = emptyList(),
    val merchantBreakdown: List<MerchantBreakdown> = emptyList(),
    val recurringSeries: List<RecurringSeries> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val month = state.currentMonth

            val startOfMonth = month.atDay(1)
            val endOfMonth = month.atEndOfMonth()

            val previousMonth = month.minusMonths(1)
            val startOfPreviousMonth = previousMonth.atDay(1)
            val endOfPreviousMonth = previousMonth.atEndOfMonth()

            val totalExpense = transactionRepository.getExpenseTotal(startOfMonth, endOfMonth)
            val totalIncome = transactionRepository.getIncomeTotal(startOfMonth, endOfMonth)
            val previousExpense = transactionRepository.getExpenseTotal(startOfPreviousMonth, endOfPreviousMonth)
            val previousIncome = transactionRepository.getIncomeTotal(startOfPreviousMonth, endOfPreviousMonth)

            val categoryBreakdown = transactionRepository.getCategoryBreakdown(startOfMonth, endOfMonth)
                .filter { it.categoryId > 0 }
                .sortedByDescending { it.total }
                .map { cat ->
                    val category = categoryRepository.getById(cat.categoryId)
                    CategoryAnalytics(
                        categoryId = cat.categoryId,
                        categoryName = cat.categoryName,
                        total = cat.total,
                        percentage = if (totalExpense > 0) (cat.total.toFloat() / totalExpense * 100) else 0f,
                        color = category?.colorHex?.let { hex ->
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                        } ?: androidx.compose.ui.graphics.Color.Unspecified
                    )
                }

            val merchantBreakdown = transactionRepository.getMerchantBreakdown(startOfMonth, endOfMonth, 10)

            val lookbackStart = month.atEndOfMonth().minusMonths(12)
            val lookbackEnd = month.atEndOfMonth()
            val recent = transactionRepository.getForDateRange(lookbackStart, lookbackEnd)
            val recurring = RecurringDetector.detect(recent)

            _uiState.update {
                it.copy(
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    previousExpense = previousExpense,
                    previousIncome = previousIncome,
                    categoryBreakdown = categoryBreakdown,
                    merchantBreakdown = merchantBreakdown,
                    recurringSeries = recurring,
                    isLoading = false
                )
            }
        }
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
        loadAnalytics()
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
        loadAnalytics()
    }
}
