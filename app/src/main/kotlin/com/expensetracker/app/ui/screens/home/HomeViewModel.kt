package com.expensetracker.app.ui.screens.home

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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val monthName: String = "",
    val totalExpense: Long = 0,
    val totalIncome: Long = 0,
    val budgetRemaining: Long? = null,
    val budgetTotal: Long? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val transactionsThisMonth: Int = 0,
    val activeDays: Int = 0,
    val streakDays: Int = 0,
    val momentumScore: Int = 0,
    val focusRank: String = "Getting Started",
    val missionProgress: Float = 0f,
    val missionLabel: String = "Log your first transaction to start building your month.",
    val netFlow: Long = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
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

            transactionRepository.observeForDateRange(startOfMonth, endOfMonth).collect { transactions ->
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
                val missionProgress = (transactionCount / 12f).coerceIn(0f, 1f)
                val momentumScore = (
                    activeDays * 8 +
                        transactionCount.coerceAtMost(12) * 4 +
                        if (totalIncome > 0) 14 else 0 +
                        if (totalExpense > 0) 10 else 0
                    ).coerceAtMost(100)

                _uiState.update {
                    it.copy(
                        monthName = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        totalExpense = totalExpense,
                        totalIncome = totalIncome,
                        recentTransactions = transactions
                            .sortedByDescending { tx -> tx.transactionTime }
                            .take(10),
                        transactionsThisMonth = transactionCount,
                        activeDays = activeDays,
                        streakDays = streakDays,
                        momentumScore = momentumScore,
                        focusRank = rankFor(momentumScore),
                        missionProgress = missionProgress,
                        missionLabel = missionLabelFor(transactionCount, streakDays),
                        netFlow = totalIncome - totalExpense,
                        isLoading = false
                    )
                }
            }
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

    private fun rankFor(score: Int): String {
        return when {
            score >= 85 -> "Excellent"
            score >= 65 -> "Strong"
            score >= 40 -> "Steady"
            score >= 15 -> "Building"
            else -> "Getting Started"
        }
    }

    private fun missionLabelFor(transactionCount: Int, streakDays: Int): String {
        return when {
            transactionCount == 0 -> "Log your first transaction to start building your month."
            streakDays >= 5 -> "Nice consistency. Keep your streak going."
            transactionCount < 5 -> "A few more entries will make your monthly view more useful."
            transactionCount < 12 -> "You are building a solid picture of your spending."
            else -> "Great coverage this month. Your dashboard is well populated."
        }
    }
}
