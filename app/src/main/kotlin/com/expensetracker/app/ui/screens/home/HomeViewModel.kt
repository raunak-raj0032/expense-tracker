package com.expensetracker.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.capture.CaptureEventRepository
import com.expensetracker.app.core.data.repository.BudgetRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val monthName: String = "",
    val totalExpense: Long = 0,
    val totalIncome: Long = 0,
    val budgetRemaining: Long? = null,
    val budgetTotal: Long? = null,
    val budgetName: String? = null,
    val todayExpense: Long = 0,
    val todayBudgetAllowance: Long? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val transactionsThisMonth: Int = 0,
    val activeDays: Int = 0,
    val streakDays: Int = 0,
    val dayOfMonth: Int = 1,
    val daysInMonth: Int = 30,
    val netFlow: Long = 0,
    val openCaptureCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val captureEventRepository: CaptureEventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val now = LocalDate.now()
            val month = YearMonth.from(now)
            val startOfMonth = month.atDay(1)
            val endOfMonth = month.atEndOfMonth()
            val dayOfMonth = now.dayOfMonth
            val daysInMonth = month.lengthOfMonth()

            combine(
                transactionRepository.observeForDateRange(startOfMonth, endOfMonth),
                budgetRepository.observeMonthlyBudgetFor(month),
                captureEventRepository.observeOpenCount()
            ) { transactions, budget, openCaptureCount ->
                val totalExpense = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amountMinor }
                val totalIncome = transactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amountMinor }
                val activeDates = transactions
                    .map { it.transactionTime.toLocalDate() }
                    .toSet()
                val activeDays = activeDates.size
                val streakDays = calculateStreak(activeDates)
                val transactionCount = transactions.size
                val todayExpense = transactions
                    .filter {
                        it.type == TransactionType.EXPENSE &&
                            it.transactionTime.toLocalDate() == now
                    }
                    .sumOf { it.amountMinor }
                val budgetTotal = budget?.amountMinor
                val budgetRemaining = budgetTotal?.minus(totalExpense)
                val todayBudgetAllowance = budgetTotal?.let {
                    budgetAllowanceForDay(
                        totalMinor = it,
                        totalDays = daysInMonth,
                        dayNumber = dayOfMonth
                    )
                }

                _uiState.update {
                    it.copy(
                        monthName = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        totalExpense = totalExpense,
                        totalIncome = totalIncome,
                        budgetTotal = budgetTotal,
                        budgetRemaining = budgetRemaining,
                        budgetName = budget?.name,
                        todayExpense = todayExpense,
                        todayBudgetAllowance = todayBudgetAllowance,
                        recentTransactions = transactions
                            .sortedByDescending { tx -> tx.transactionTime }
                            .take(10),
                        transactionsThisMonth = transactionCount,
                        activeDays = activeDays,
                        streakDays = streakDays,
                        dayOfMonth = dayOfMonth,
                        daysInMonth = daysInMonth,
                        netFlow = totalIncome - totalExpense,
                        openCaptureCount = openCaptureCount,
                        isLoading = false
                    )
                }
            }.collect { }
        }
    }

    private fun calculateStreak(activeDates: Set<LocalDate>): Int {
        if (activeDates.isEmpty()) {
            return 0
        }

        var cursor = activeDates.maxOrNull() ?: return 0
        var streak = 0
        while (cursor in activeDates) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun budgetAllowanceForDay(
        totalMinor: Long,
        totalDays: Int,
        dayNumber: Int
    ): Long {
        if (totalMinor <= 0 || totalDays <= 0) {
            return 0
        }

        val safeDay = dayNumber.coerceIn(1, totalDays)
        val basePerDay = totalMinor / totalDays
        val remainder = totalMinor % totalDays
        return basePerDay + if (safeDay.toLong() <= remainder) 1 else 0
    }
}
