package com.expensetracker.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.domain.RecurringDetector
import com.expensetracker.app.core.domain.RecurringSeries
import com.expensetracker.app.core.model.MerchantBreakdown
import com.expensetracker.app.ai.OnDeviceAiManager
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
    val aiInsight: String? = null,
    val aiInsightError: String? = null,
    val aiInsightLoading: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val aiManager: OnDeviceAiManager
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
                    CategoryAnalytics(
                        categoryId = cat.categoryId,
                        categoryName = cat.categoryName,
                        total = cat.total,
                        percentage = if (totalExpense > 0) (cat.total.toFloat() / totalExpense * 100) else 0f
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

    fun generateAiInsights() {
        val snapshot = _uiState.value
        if (snapshot.aiInsightLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(aiInsightLoading = true, aiInsightError = null) }
            runCatching {
                aiManager.initializeIfNeeded().getOrThrow()
                val conversation = aiManager.createConversation()
                try {
                    conversation.respond(buildInsightPrompt(snapshot))
                } finally {
                    conversation.close()
                }
            }.onSuccess { insight ->
                _uiState.update {
                    it.copy(
                        aiInsight = insight.ifBlank { "No clear insight was generated." },
                        aiInsightLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        aiInsightError = error.message ?: "Could not reach your laptop AI host.",
                        aiInsightLoading = false
                    )
                }
            }
        }
    }

    private fun buildInsightPrompt(state: AnalyticsUiState): String {
        val categoryLines = state.categoryBreakdown.take(8).joinToString("\n") {
            "- ${it.categoryName}: ${it.total / 100.0} (${String.format("%.1f", it.percentage)}%)"
        }.ifBlank { "- No category data" }
        val merchantLines = state.merchantBreakdown.take(8).joinToString("\n") {
            "- ${it.merchantName}: ${it.total / 100.0} across ${it.transactionCount} transactions"
        }.ifBlank { "- No merchant data" }
        val recurringLines = state.recurringSeries.take(6).joinToString("\n") {
            "- ${it.description}: avg ${it.averageAmountMinor / 100.0}, ${it.cadence.label}, next ${it.nextExpected}"
        }.ifBlank { "- No recurring charges detected" }

        return """
            You are a practical personal finance analyst for an Indian expense tracker app.
            Use only the numbers below. Do not invent transactions.
            Write concise insights for the user in 4 bullets:
            1. What changed or stands out.
            2. The biggest spending drivers.
            3. Any recurring/subscription warning.
            4. One specific action for the next 7 days.

            Month: ${state.currentMonth}
            Expense: ${state.totalExpense / 100.0}
            Income: ${state.totalIncome / 100.0}
            Net flow: ${(state.totalIncome - state.totalExpense) / 100.0}
            Previous month expense: ${state.previousExpense / 100.0}
            Previous month income: ${state.previousIncome / 100.0}

            Categories:
            $categoryLines

            Merchants:
            $merchantLines

            Recurring:
            $recurringLines
        """.trimIndent()
    }
}
