package com.expensetracker.app.core.domain

import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.core.model.TransactionType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class RecurringCadence(val days: Int, val label: String) {
    WEEKLY(7, "Weekly"),
    BIWEEKLY(14, "Bi-weekly"),
    MONTHLY(30, "Monthly"),
    QUARTERLY(91, "Quarterly"),
    YEARLY(365, "Yearly")
}

data class RecurringSeries(
    val signature: String,
    val description: String,
    val cadence: RecurringCadence,
    val averageAmountMinor: Long,
    val occurrences: Int,
    val lastSeen: LocalDate,
    val nextExpected: LocalDate,
    val categoryId: Long?
)

object RecurringDetector {
    private const val MIN_OCCURRENCES = 3
    private const val CADENCE_TOLERANCE_DAYS = 4
    private const val AMOUNT_TOLERANCE_PCT = 0.12

    fun detect(transactions: List<Transaction>, @Suppress("UNUSED_PARAMETER") today: LocalDate = LocalDate.now()): List<RecurringSeries> {
        val candidates = transactions
            .filter { it.type == TransactionType.EXPENSE && !it.description.isNullOrBlank() }
            .groupBy { signatureFor(it) }
            .filterValues { it.size >= MIN_OCCURRENCES }

        return candidates.mapNotNull { (sig, txs) ->
            val sorted = txs.sortedBy { it.transactionTime }
            val dates = sorted.map { it.transactionTime.toLocalDate() }
            val gaps = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            if (gaps.isEmpty()) return@mapNotNull null

            val avgGap = gaps.average()
            val cadence = RecurringCadence.entries.minByOrNull { abs(it.days - avgGap) } ?: return@mapNotNull null
            if (abs(cadence.days - avgGap) > CADENCE_TOLERANCE_DAYS) return@mapNotNull null

            val amounts = sorted.map { it.amountMinor }
            val avgAmount = amounts.average().toLong()
            val within = amounts.all { abs(it - avgAmount) <= (avgAmount * AMOUNT_TOLERANCE_PCT).toLong().coerceAtLeast(50L) }
            if (!within) return@mapNotNull null

            val last = dates.last()
            val next = last.plusDays(cadence.days.toLong())
            val recentCategory = sorted.lastOrNull { it.categoryId != null }?.categoryId

            RecurringSeries(
                signature = sig,
                description = sorted.last().description.orEmpty(),
                cadence = cadence,
                averageAmountMinor = avgAmount,
                occurrences = sorted.size,
                lastSeen = last,
                nextExpected = next,
                categoryId = recentCategory
            )
        }.sortedByDescending { it.occurrences }
    }

    private fun signatureFor(tx: Transaction): String {
        val desc = tx.description?.trim()?.lowercase()
            ?.replace(Regex("\\d+"), "")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
        return desc
    }
}
