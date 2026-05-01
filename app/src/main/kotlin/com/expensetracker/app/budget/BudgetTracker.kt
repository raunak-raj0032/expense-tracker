package com.expensetracker.app.budget

import com.expensetracker.app.core.data.repository.BudgetRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

enum class BudgetPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    fun next(): BudgetPeriod = when (this) {
        DAILY -> WEEKLY
        WEEKLY -> MONTHLY
        MONTHLY -> DAILY
    }

    companion object {
        fun fromName(name: String?): BudgetPeriod =
            entries.firstOrNull { it.name == name } ?: MONTHLY
    }
}

data class BudgetSnapshot(
    val period: BudgetPeriod,
    val periodLabel: String,
    val budgetMinor: Long?,
    val spentMinor: Long,
    val remainingMinor: Long?,
    val progressFraction: Float
) {
    val hasBudget: Boolean get() = budgetMinor != null && budgetMinor > 0
}

@Singleton
class BudgetTracker @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(period: BudgetPeriod, today: LocalDate = LocalDate.now()): Flow<BudgetSnapshot> {
        val (start, end, label) = rangeFor(period, today)
        val month = YearMonth.from(today)
        return budgetRepository.observeMonthlyBudgetFor(month).flatMapLatest { budget ->
            val monthlyMinor = budget?.amountMinor
            val periodBudgetMinor = monthlyMinor?.let { allocateForPeriod(it, period, today) }
            transactionRepository.observeForDateRange(start, end).let { txFlow ->
                combine(txFlow, flowOf(periodBudgetMinor)) { txs, pb ->
                    val spent = txs.filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amountMinor }
                    val remaining = pb?.minus(spent)
                    val progress = if (pb != null && pb > 0) {
                        (spent.toFloat() / pb.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    BudgetSnapshot(
                        period = period,
                        periodLabel = label,
                        budgetMinor = pb,
                        spentMinor = spent,
                        remainingMinor = remaining,
                        progressFraction = progress
                    )
                }
            }
        }
    }

    private fun rangeFor(period: BudgetPeriod, today: LocalDate): Triple<LocalDate, LocalDate, String> {
        return when (period) {
            BudgetPeriod.DAILY -> Triple(today, today, "Today")
            BudgetPeriod.WEEKLY -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = start.plusDays(6)
                Triple(start, end, "This week")
            }
            BudgetPeriod.MONTHLY -> {
                val month = YearMonth.from(today)
                Triple(month.atDay(1), month.atEndOfMonth(), "This month")
            }
        }
    }

    private fun allocateForPeriod(monthlyMinor: Long, period: BudgetPeriod, today: LocalDate): Long {
        val month = YearMonth.from(today)
        val days = month.lengthOfMonth()
        return when (period) {
            BudgetPeriod.MONTHLY -> monthlyMinor
            BudgetPeriod.DAILY -> monthlyMinor / days
            BudgetPeriod.WEEKLY -> (monthlyMinor * 7) / days
        }
    }
}
