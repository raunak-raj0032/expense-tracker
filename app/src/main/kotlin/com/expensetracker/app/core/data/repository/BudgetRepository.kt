package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.BudgetDao
import com.expensetracker.app.core.database.entity.BudgetEntity
import com.expensetracker.app.core.model.Budget
import com.expensetracker.app.core.model.BudgetPeriodType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    fun observeLatestMonthly(): Flow<Budget?> = budgetDao.observeLatestMonthly().map { entity ->
        entity?.toDomain()
    }

    fun observeMonthlyBudgetFor(month: YearMonth): Flow<Budget?> {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        return observeLatestMonthly().map { budget ->
            budget?.takeIf {
                it.startDate <= end && (it.endDate == null || it.endDate >= start)
            }
        }
    }

    suspend fun upsertMonthlyBudget(
        amountMinor: Long,
        name: String = "Monthly Budget",
        month: YearMonth = YearMonth.now()
    ): Long {
        val startDate = month.atDay(1)
        val existing = budgetDao.getLatestEnabledByPeriod(BudgetPeriodType.MONTHLY.name)
            ?.takeIf {
                val existingStart = it.startDate.toLocalDate()
                val existingEnd = it.endDate?.toLocalDate()
                existingStart <= month.atEndOfMonth() && (existingEnd == null || existingEnd >= month.atDay(1))
            }

        val entity = BudgetEntity(
            id = existing?.id ?: 0,
            name = name,
            periodType = BudgetPeriodType.MONTHLY.name,
            amountMinor = amountMinor,
            startDate = startDate.toEpochMillis(),
            endDate = month.atEndOfMonth().toEpochMillis(),
            isEnabled = true
        )

        return if (existing != null) {
            budgetDao.update(entity)
            existing.id
        } else {
            budgetDao.insert(entity)
        }
    }

    private fun BudgetEntity.toDomain(): Budget {
        return Budget(
            id = id,
            name = name,
            periodType = BudgetPeriodType.valueOf(periodType),
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            categoryId = categoryId,
            accountId = accountId,
            tagId = tagId,
            startDate = startDate.toLocalDate(),
            endDate = endDate?.toLocalDate(),
            isEnabled = isEnabled
        )
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private fun LocalDate.toEpochMillis(): Long {
        return atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
