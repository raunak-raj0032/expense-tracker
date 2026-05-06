package com.expensetracker.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TagRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.dao.TransactionTagDao
import com.expensetracker.app.core.domain.RecurringDetector
import com.expensetracker.app.core.domain.RecurringSeries
import com.expensetracker.app.core.model.TransactionStatus
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.ai.OnDeviceAiManager
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class CategoryAnalytics(
    val categoryId: Long,
    val categoryName: String,
    val total: Long,
    val percentage: Float,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
)

data class TagAnalytics(
    val tagId: Long,
    val tagName: String,
    val colorHex: String,
    val total: Long,
    val transactionCount: Int,
    val percentage: Float
)

data class DailyPoint(val day: Int, val expenseMinor: Long, val incomeMinor: Long)
data class WeekdayPoint(val label: String, val expenseMinor: Long)
data class SizeBucket(val label: String, val range: String, val count: Int, val totalMinor: Long)
data class PaymentSlice(val method: String, val totalMinor: Long, val fraction: Float)
data class MonthlyPoint(val month: YearMonth, val expenseMinor: Long, val incomeMinor: Long)

data class AnalyticsUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val totalExpense: Long = 0,
    val totalIncome: Long = 0,
    val previousExpense: Long = 0,
    val previousIncome: Long = 0,
    val transactionCount: Int = 0,
    val avgDailySpend: Long = 0,
    val largestExpense: Long = 0,
    val categoryBreakdown: List<CategoryAnalytics> = emptyList(),
    val tagBreakdown: List<TagAnalytics> = emptyList(),
    val recurringSeries: List<RecurringSeries> = emptyList(),
    val dailyPoints: List<DailyPoint> = emptyList(),
    val weekdayPoints: List<WeekdayPoint> = emptyList(),
    val sizeBuckets: List<SizeBucket> = emptyList(),
    val paymentSlices: List<PaymentSlice> = emptyList(),
    val monthlyTrend: List<MonthlyPoint> = emptyList(),
    val aiInsight: String? = null,
    val aiInsightError: String? = null,
    val aiInsightLoading: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
    private val transactionTagDao: TransactionTagDao,
    private val aiManager: OnDeviceAiManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init { loadAnalytics() }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val month = _uiState.value.currentMonth
            val startOfMonth = month.atDay(1)
            val endOfMonth = month.atEndOfMonth()
            val prevMonth = month.minusMonths(1)

            val totalExpense = transactionRepository.getExpenseTotal(startOfMonth, endOfMonth)
            val totalIncome = transactionRepository.getIncomeTotal(startOfMonth, endOfMonth)
            val previousExpense = transactionRepository.getExpenseTotal(prevMonth.atDay(1), prevMonth.atEndOfMonth())
            val previousIncome = transactionRepository.getIncomeTotal(prevMonth.atDay(1), prevMonth.atEndOfMonth())

            val categoryBreakdown = transactionRepository.getCategoryBreakdown(startOfMonth, endOfMonth)
                .filter { it.categoryId > 0 }
                .sortedByDescending { it.total }
                .map { cat ->
                    CategoryAnalytics(
                        categoryId   = cat.categoryId,
                        categoryName = cat.categoryName,
                        total        = cat.total,
                        percentage   = if (totalExpense > 0) (cat.total.toFloat() / totalExpense * 100) else 0f
                    )
                }

            val monthTxs = transactionRepository.getForDateRange(startOfMonth, endOfMonth)
                .filter { it.status == TransactionStatus.CONFIRMED }
            val expenses = monthTxs.filter { it.type == TransactionType.EXPENSE }

            // Tag breakdown
            val allTags = tagRepository.observeAll().first().associateBy { it.id }
            val txIds = expenses.map { it.id }
            val tagTotals = mutableMapOf<Long, Long>()
            val tagCounts = mutableMapOf<Long, Int>()
            if (txIds.isNotEmpty()) {
                transactionTagDao.getForTransactions(txIds).forEach { join ->
                    val tx = expenses.find { it.id == join.transactionId } ?: return@forEach
                    tagTotals[join.tagId] = (tagTotals[join.tagId] ?: 0L) + tx.amountMinor
                    tagCounts[join.tagId] = (tagCounts[join.tagId] ?: 0) + 1
                }
            }
            val tagBreakdown = tagTotals.entries
                .sortedByDescending { it.value }
                .mapNotNull { (tagId, total) ->
                    val tag = allTags[tagId] ?: return@mapNotNull null
                    TagAnalytics(
                        tagId            = tagId,
                        tagName          = tag.name,
                        colorHex         = tag.colorHex,
                        total            = total,
                        transactionCount = tagCounts[tagId] ?: 0,
                        percentage       = if (totalExpense > 0) total.toFloat() / totalExpense * 100f else 0f
                    )
                }

            // Per-day aggregates
            val expByDay = mutableMapOf<Int, Long>()
            val incByDay = mutableMapOf<Int, Long>()
            for (tx in monthTxs) {
                val d = tx.transactionTime.dayOfMonth
                if (tx.type == TransactionType.EXPENSE) expByDay[d] = (expByDay[d] ?: 0L) + tx.amountMinor
                else if (tx.type == TransactionType.INCOME) incByDay[d] = (incByDay[d] ?: 0L) + tx.amountMinor
            }
            val dailyPoints = (1..month.lengthOfMonth()).map { d ->
                DailyPoint(d, expByDay.getOrDefault(d, 0L), incByDay.getOrDefault(d, 0L))
            }

            // Day-of-week pattern (Mon=0 .. Sun=6)
            val dowTotals = LongArray(7)
            for (tx in expenses) dowTotals[tx.transactionTime.dayOfWeek.value - 1] += tx.amountMinor
            val weekdayPoints = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                .mapIndexed { i, label -> WeekdayPoint(label, dowTotals[i]) }

            // Transaction-size buckets (amounts in paise)
            val b1 = expenses.filter { it.amountMinor < 10_000L }
            val b2 = expenses.filter { it.amountMinor in 10_000L..99_999L }
            val b3 = expenses.filter { it.amountMinor in 100_000L..499_999L }
            val b4 = expenses.filter { it.amountMinor >= 500_000L }
            val sizeBuckets = listOf(
                SizeBucket("Quick",   "< ₹100",    b1.size, b1.sumOf { it.amountMinor }),
                SizeBucket("Regular", "₹100–₹1K",  b2.size, b2.sumOf { it.amountMinor }),
                SizeBucket("Notable", "₹1K–₹5K",   b3.size, b3.sumOf { it.amountMinor }),
                SizeBucket("Large",   "> ₹5K",      b4.size, b4.sumOf { it.amountMinor })
            )

            // Payment method breakdown
            val payMap = mutableMapOf<String, Long>()
            for (tx in monthTxs) {
                val m = tx.paymentMethod?.trim()?.takeIf { it.isNotBlank() } ?: continue
                payMap[m] = (payMap[m] ?: 0L) + tx.amountMinor
            }
            val payTotal = payMap.values.sum()
            val paymentSlices = payMap.entries
                .sortedByDescending { it.value }
                .take(6)
                .map { (m, a) -> PaymentSlice(m, a, if (payTotal > 0) a.toFloat() / payTotal else 0f) }

            // 6-month trend + recurring via 12-month lookback
            val lookbackStart = month.minusMonths(11).atDay(1)
            val recent = transactionRepository.getForDateRange(lookbackStart, endOfMonth)
                .filter { it.status == TransactionStatus.CONFIRMED }
            val recurring = RecurringDetector.detect(recent)

            val monthlyTrend = (5 downTo 0).map { back ->
                val m = month.minusMonths(back.toLong())
                val mS = m.atDay(1); val mE = m.atEndOfMonth()
                val txs = recent.filter { tx ->
                    val d = tx.transactionTime.toLocalDate()
                    !d.isBefore(mS) && !d.isAfter(mE)
                }
                MonthlyPoint(
                    month        = m,
                    expenseMinor = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor },
                    incomeMinor  = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
                )
            }

            val activeDays = dailyPoints.count { it.expenseMinor > 0 }

            _uiState.update {
                it.copy(
                    totalExpense     = totalExpense,
                    totalIncome      = totalIncome,
                    previousExpense  = previousExpense,
                    previousIncome   = previousIncome,
                    transactionCount = monthTxs.size,
                    avgDailySpend    = if (activeDays > 0) totalExpense / activeDays else 0L,
                    largestExpense   = expenses.maxOfOrNull { e -> e.amountMinor } ?: 0L,
                    categoryBreakdown = categoryBreakdown,
                    tagBreakdown      = tagBreakdown,
                    recurringSeries  = recurring,
                    dailyPoints      = dailyPoints,
                    weekdayPoints    = weekdayPoints,
                    sizeBuckets      = sizeBuckets,
                    paymentSlices    = paymentSlices,
                    monthlyTrend     = monthlyTrend,
                    isLoading        = false
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
                val conv = aiManager.createConversation()
                try { conv.respond(buildInsightPrompt(snapshot)) } finally { conv.close() }
            }.onSuccess { insight ->
                _uiState.update { it.copy(aiInsight = insight.ifBlank { "No clear insight generated." }, aiInsightLoading = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(aiInsightError = err.message ?: "Could not reach AI host.", aiInsightLoading = false) }
            }
        }
    }

    private fun buildInsightPrompt(s: AnalyticsUiState): String {
        val catLines = s.categoryBreakdown.take(8)
            .joinToString("\n") { "- ${it.categoryName}: ₹${it.total / 100} (${String.format("%.1f", it.percentage)}%)" }
            .ifBlank { "- No category data" }
        val tagLines = s.tagBreakdown.take(8)
            .joinToString("\n") { "- ${it.tagName}: ₹${it.total / 100} × ${it.transactionCount} txns" }
            .ifBlank { "- No tag data" }
        val recLines = s.recurringSeries.take(6)
            .joinToString("\n") { "- ${it.description}: avg ₹${it.averageAmountMinor / 100}, ${it.cadence.label}" }
            .ifBlank { "- None detected" }
        val dowLine = s.weekdayPoints.joinToString(", ") { "${it.label}: ₹${it.expenseMinor / 100}" }
        val bucketLine = s.sizeBuckets.joinToString(" | ") { "${it.label} ${it.count}×" }

        return """
            Personal finance analyst for Indian expense tracker. Be concise, 4 bullets:
            1. Notable change vs last month.
            2. Biggest spending drivers.
            3. Subscription/recurring warning if any.
            4. One specific action for next 7 days.

            ${s.currentMonth} | Spent: ₹${s.totalExpense / 100} | Income: ₹${s.totalIncome / 100} | Net: ₹${(s.totalIncome - s.totalExpense) / 100}
            Prev month spent: ₹${s.previousExpense / 100}
            Transactions: ${s.transactionCount} | Avg/active day: ₹${s.avgDailySpend / 100} | Largest: ₹${s.largestExpense / 100}
            Day-of-week: $dowLine
            Size mix: $bucketLine
            Categories: $catLines
            Tags: $tagLines
            Recurring: $recLines
        """.trimIndent()
    }
}
