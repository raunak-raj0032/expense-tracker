package com.expensetracker.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.model.CalendarDay
import com.expensetracker.app.core.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val calendarDays: Map<LocalDate, CalendarDay> = emptyMap(),
    val dailyTotals: Map<LocalDate, CalendarDay> = emptyMap(),
    val periodDays: List<CalendarDay> = emptyList(),
    val transactionsForSelectedDate: List<Transaction> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val periodStart: LocalDate = LocalDate.now(),
    val periodEnd: LocalDate = LocalDate.now(),
    val periodExpenseTotal: Long = 0,
    val periodIncomeTotal: Long = 0,
    val periodTransactionCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadCalendarData(YearMonth.now())
    }

    fun loadCalendarData(yearMonth: YearMonth) {
        loadRangeData(
            startDate = yearMonth.atDay(1),
            endDate = yearMonth.atEndOfMonth()
        )
    }

    fun loadRangeData(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    periodStart = startDate,
                    periodEnd = endDate
                )
            }

            val transactions = transactionRepository.getForDateRange(startDate, endDate)
            val dailyTotals = transactions.groupBy { it.transactionTime.toLocalDate() }
                .mapValues { (_, txs) ->
                    val expense = txs.filter { it.type.name == "EXPENSE" }.sumOf { it.amountMinor }
                    val income = txs.filter { it.type.name == "INCOME" }.sumOf { it.amountMinor }
                    CalendarDay(
                        date = txs.first().transactionTime.toLocalDate(),
                        expenseTotal = expense,
                        incomeTotal = income,
                        netTotal = income - expense,
                        transactionCount = txs.size
                    )
                }

            val periodDays = generateSequence(startDate) { current ->
                current.plusDays(1).takeIf { it <= endDate }
            }.map { date ->
                dailyTotals[date] ?: CalendarDay(
                    date = date,
                    expenseTotal = 0,
                    incomeTotal = 0,
                    netTotal = 0,
                    transactionCount = 0
                )
            }.toList()

            _uiState.update {
                it.copy(
                    calendarDays = dailyTotals,
                    dailyTotals = dailyTotals,
                    periodDays = periodDays,
                    periodExpenseTotal = periodDays.sumOf { day -> day.expenseTotal },
                    periodIncomeTotal = periodDays.sumOf { day -> day.incomeTotal },
                    periodTransactionCount = transactions.size,
                    isLoading = false
                )
            }
        }
    }

    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedDate = date, isLoading = true) }

            val transactions = transactionRepository.getForDateRange(date, date)
                .sortedByDescending { it.transactionTime }

            _uiState.update {
                it.copy(
                    transactionsForSelectedDate = transactions,
                    isLoading = false
                )
            }
        }
    }
}
