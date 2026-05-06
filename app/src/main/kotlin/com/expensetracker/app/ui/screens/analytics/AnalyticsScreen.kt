package com.expensetracker.app.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.AccentDivider
import com.expensetracker.app.ui.theme.MainTopBar
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.GlowProgressBar
import com.expensetracker.app.ui.theme.NeonPill
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.CardSpacing
import com.expensetracker.app.ui.theme.SectionSpacing
import com.expensetracker.app.ui.theme.financialFigures
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); visible = true }

    val isCurrentMonth = uiState.currentMonth == YearMonth.now()
    val todayDay = if (isCurrentMonth) LocalDate.now().dayOfMonth else -1

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            MainTopBar(
                title    = "Money Story",
                subtitle = "Trends & deep-dive analytics",
                accent   = MaterialTheme.colorScheme.secondary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(
                start  = ScreenEdgePadding, top = CardSpacing,
                end    = ScreenEdgePadding, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            // Month picker
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400, easing = FastOutSlowInEasing)) { -20 }) {
                    MonthSelector(uiState.currentMonth, viewModel::previousMonth, viewModel::nextMonth)
                }
            }

            // Overview card
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(450, 80)) + slideInVertically(tween(450, 80, FastOutSlowInEasing)) { 24 }) {
                    SummaryCard(
                        totalExpense    = uiState.totalExpense,
                        totalIncome     = uiState.totalIncome,
                        netFlow         = uiState.totalIncome - uiState.totalExpense,
                        previousExpense = uiState.previousExpense,
                        previousIncome  = uiState.previousIncome
                    )
                }
            }

            // Quick stats strip
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 100))) {
                    QuickStatsRow(uiState.transactionCount, uiState.avgDailySpend, uiState.largestExpense)
                }
            }

            // 6-month trend
            if (uiState.monthlyTrend.any { it.expenseMinor > 0 || it.incomeMinor > 0 }) {
                item {
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 120))) {
                        SixMonthTrendCard(uiState.monthlyTrend, uiState.currentMonth)
                    }
                }
            }

            // Daily spend bars
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 140))) {
                    DailySpendCard(uiState.dailyPoints, uiState.currentMonth, todayDay, uiState.avgDailySpend)
                }
            }

            // Weekly rhythm
            if (uiState.weekdayPoints.any { it.expenseMinor > 0 }) {
                item {
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 160))) {
                        WeeklyRhythmCard(uiState.weekdayPoints)
                    }
                }
            }

            // Spending scale distribution
            if (uiState.totalExpense > 0) {
                item {
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 180))) {
                        SpendingScaleCard(uiState.sizeBuckets)
                    }
                }
            }

            // Payment methods
            if (uiState.paymentSlices.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 200))) {
                        PaymentMethodCard(uiState.paymentSlices)
                    }
                }
            }

            // Tags
            if (uiState.tagBreakdown.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 220))) {
                        SectionLabel("TAGS", "Spending by tag", MaterialTheme.colorScheme.tertiary)
                    }
                }
                items(uiState.tagBreakdown) { tag ->
                    TagBreakdownItem(tag.tagName, tag.colorHex, tag.total, tag.transactionCount, tag.percentage)
                }
            }

            // Categories (only shown if user has classified transactions)
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 240))) {
                        SectionLabel("CATEGORY PULSE", "Where your money goes", MaterialTheme.colorScheme.primary)
                    }
                }
                items(uiState.categoryBreakdown) { cat ->
                    CategoryBreakdownItem(cat.categoryName, cat.total, cat.percentage, cat.color)
                }
            }

            // Recurring series
            if (uiState.recurringSeries.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 260))) {
                        SectionLabel("SUBSCRIPTIONS", "Recurring charges we spotted", MaterialTheme.colorScheme.secondary)
                    }
                }
                items(uiState.recurringSeries) { series ->
                    RecurringSeriesItem(series)
                }
            }

            // AI Insight at the bottom
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(450, 280))) {
                    AiInsightCard(
                        insight    = uiState.aiInsight,
                        error      = uiState.aiInsightError,
                        loading    = uiState.aiInsightLoading,
                        onGenerate = viewModel::generateAiInsights
                    )
                }
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(eyebrow: String, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(3.dp).height(20.dp).clip(CircleShape).background(accent))
        Column {
            Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = accent, letterSpacing = 1.2.sp)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Bottom-aligned bar column for use in fixed-height chart rows. */
@Composable
private fun BarCol(
    fraction: Float,
    color: Color,
    chartHeight: Dp,
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.Unspecified
) {
    val frac = fraction.coerceIn(0f, 1f)
    val barH = if (frac > 0f) chartHeight * frac.coerceAtLeast(0.03f) else 0.dp
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(chartHeight - barH))
        if (barH > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(barH)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            fontSize  = 7.5.sp,
            color     = if (labelColor == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else labelColor,
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
    }
}

// ── Cards ──────────────────────────────────────────────────────────────────────

@Composable
private fun QuickStatsRow(count: Int, avgDaily: Long, largest: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Transactions", "$count this month", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        StatTile("Daily avg", formatAmount(avgDaily), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        StatTile("Largest", formatAmount(largest), MaterialTheme.colorScheme.error, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(accent.copy(alpha = 0.10f))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent, letterSpacing = 0.5.sp)
            Text(value, style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SixMonthTrendCard(trend: List<MonthlyPoint>, selectedMonth: YearMonth) {
    val maxVal = trend.flatMap { listOf(it.expenseMinor, it.incomeMinor) }
        .maxOrNull()?.takeIf { it > 0 } ?: 1L
    val monthFmt = DateTimeFormatter.ofPattern("MMM")
    val chartH = 64.dp

    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        SectionHeader(eyebrow = "6-MONTH TREND", title = "Income vs expenses", subtitle = "Last six months at a glance")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            trend.forEach { point ->
                val expFrac = (point.expenseMinor.toFloat() / maxVal).coerceIn(0f, 1f)
                val incFrac = (point.incomeMinor.toFloat() / maxVal).coerceIn(0f, 1f)
                val isCurrent = point.month == selectedMonth

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Grouped bars (income left, expense right) bottom-aligned
                    Box(
                        modifier = Modifier.height(chartH).fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val incH = if (incFrac > 0f) chartH * incFrac.coerceAtLeast(0.03f) else 0.dp
                            val expH = if (expFrac > 0f) chartH * expFrac.coerceAtLeast(0.03f) else 0.dp
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(if (incH > 0.dp) incH else 2.dp)
                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    .background(
                                        if (incH > 0.dp) MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
                                        else Color.Transparent
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(if (expH > 0.dp) expH else 2.dp)
                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    .background(
                                        if (expH > 0.dp) MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                                        else Color.Transparent
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = point.month.format(monthFmt),
                        style      = MaterialTheme.typography.labelSmall,
                        fontSize   = 8.sp,
                        color      = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.secondary))
                Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.error))
                Text("Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DailySpendCard(
    points: List<DailyPoint>,
    month: YearMonth,
    todayDay: Int,
    avgDailySpend: Long
) {
    val maxExp = points.maxOfOrNull { it.expenseMinor }?.takeIf { it > 0 } ?: 1L
    val chartH  = 72.dp
    val barW    = 20.dp

    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.error) {
        SectionHeader(
            eyebrow  = "DAILY SPEND",
            title    = "Day-by-day activity",
            subtitle = "Scroll to see the full month"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            points.forEach { point ->
                val isToday = point.day == todayDay
                val frac    = point.expenseMinor.toFloat() / maxExp.toFloat()
                val color = when {
                    isToday && point.expenseMinor > 0 -> MaterialTheme.colorScheme.primary
                    isToday                           -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    point.expenseMinor > 0            -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f + 0.55f * frac)
                    else                              -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
                val labelColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Unspecified
                BarCol(
                    fraction    = if (point.expenseMinor > 0) frac else 0f,
                    color       = color,
                    chartHeight = chartH,
                    label       = "${point.day}",
                    modifier    = Modifier.width(barW),
                    labelColor  = labelColor
                )
            }
        }

        if (todayDay > 0) {
            val todayAmt = points.find { it.day == todayDay }?.expenseMinor ?: 0L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeonPill(text = "Today ${formatAmount(todayAmt)}", accent = MaterialTheme.colorScheme.primary)
                if (avgDailySpend > 0)
                    NeonPill(text = "Avg ${formatAmount(avgDailySpend)}/day", accent = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeeklyRhythmCard(weekdayPoints: List<WeekdayPoint>) {
    val maxVal   = weekdayPoints.maxOfOrNull { it.expenseMinor }?.takeIf { it > 0 } ?: 1L
    val peakDay  = weekdayPoints.maxByOrNull { it.expenseMinor }?.label
    val chartH   = 80.dp

    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        SectionHeader(
            eyebrow  = "WEEKLY RHYTHM",
            title    = "Which days cost the most?",
            subtitle = if (peakDay != null) "Peaks on $peakDay" else "Spending pattern by day"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            weekdayPoints.forEach { point ->
                val frac     = (point.expenseMinor.toFloat() / maxVal).coerceIn(0f, 1f)
                val isPeak   = point.label == peakDay && point.expenseMinor > 0
                val barColor = if (isPeak) MaterialTheme.colorScheme.tertiary
                               else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f + 0.45f * frac)
                BarCol(
                    fraction    = frac,
                    color       = barColor,
                    chartHeight = chartH,
                    label       = point.label,
                    modifier    = Modifier.weight(1f),
                    labelColor  = if (isPeak) MaterialTheme.colorScheme.tertiary else Color.Unspecified
                )
            }
        }

        // Show amounts for the peak day
        val peak = weekdayPoints.maxByOrNull { it.expenseMinor }
        if (peak != null && peak.expenseMinor > 0) {
            AccentDivider(accent = MaterialTheme.colorScheme.tertiary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${peak.label} total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatAmount(peak.expenseMinor), style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.Bold), color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun SpendingScaleCard(buckets: List<SizeBucket>) {
    val maxCount = buckets.maxOfOrNull { it.count }.takeIf { it != null && it > 0 } ?: 1
    val total    = buckets.sumOf { it.count }
    val accent   = listOf(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.error
    )

    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
        SectionHeader(
            eyebrow  = "SPENDING SCALE",
            title    = "Transaction size mix",
            subtitle = "$total transactions this month"
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            buckets.forEachIndexed { i, bucket ->
                val frac         = if (maxCount > 0) bucket.count.toFloat() / maxCount else 0f
                val bucketAccent = accent[i]
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = bucket.label,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = bucketAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.width(48.dp)
                    )
                    // Progress bar track
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(bucketAccent.copy(alpha = 0.85f))
                        )
                    }
                    Text(
                        text     = bucket.range,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(68.dp),
                        maxLines = 1
                    )
                    Text(
                        text       = "${bucket.count}×",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = bucketAccent,
                        modifier   = Modifier.width(28.dp),
                        textAlign  = TextAlign.End
                    )
                }
                if (bucket.totalMinor > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 56.dp)) {
                        Text(
                            text  = formatAmount(bucket.totalMinor),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(slices: List<PaymentSlice>) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.primaryContainer
    )

    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        SectionHeader(
            eyebrow  = "HOW YOU PAY",
            title    = "Payment method split",
            subtitle = "${slices.size} methods used this month"
        )

        // Segmented proportional bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            slices.forEachIndexed { i, slice ->
                Box(
                    modifier = Modifier
                        .weight(slice.fraction.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(colors[i % colors.size])
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Legend rows
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.forEachIndexed { i, slice ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors[i % colors.size]))
                    Text(
                        text     = slice.method,
                        style    = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text  = formatAmount(slice.totalMinor),
                        style = MaterialTheme.typography.bodySmall.financialFigures(FontWeight.Bold),
                        color = colors[i % colors.size]
                    )
                    Text(
                        text     = "${(slice.fraction * 100).roundToInt()}%",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun AiInsightCard(insight: String?, error: String?, loading: Boolean, onGenerate: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                eyebrow  = "AI INSIGHTS",
                title    = "AI read on your month",
                subtitle = "Uses your configured AI provider"
            )
            when {
                loading  -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Asking llama3.2...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                error != null  -> Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                insight != null -> Text(insight, style = MaterialTheme.typography.bodyMedium)
                else -> Text(
                    "Generate a short spending analysis covering trends, top spending areas, and one actionable suggestion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onGenerate,
                enabled = !loading,
                colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (insight == null) "Generate insights" else "Refresh insights")
            }
        }
    }
}

// ── Existing cards kept ────────────────────────────────────────────────────────

@Composable
fun MonthSelector(currentMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        SectionHeader(
            eyebrow  = "Monthly View",
            title    = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            subtitle = "Navigate to compare months",
            trailing = {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                    Row {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous", tint = MaterialTheme.colorScheme.primary)
                        }
                        Box(Modifier.width(1.dp).height(32.dp).align(Alignment.CenterVertically).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SummaryCard(totalExpense: Long, totalIncome: Long, netFlow: Long, previousExpense: Long, previousIncome: Long) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
        SectionHeader(eyebrow = "Overview", title = "Monthly balance", subtitle = "Spend, income, and trend signals")

        val flowAccent = if (netFlow >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val spendRatio = if (totalIncome > 0L) (totalExpense.toFloat() / totalIncome.toFloat()).coerceIn(0f, 1f) else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Brush.linearGradient(listOf(flowAccent.copy(alpha = 0.12f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("NET FLOW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.0.sp)
                        Text(
                            text  = "${if (netFlow >= 0) "+" else ""}${formatAmount(abs(netFlow))}",
                            style = MaterialTheme.typography.displaySmall.financialFigures(FontWeight.Black),
                            color = flowAccent
                        )
                    }
                    NeonPill(
                        text   = when { netFlow > 0L -> "Positive"; netFlow < 0L -> "Overspending"; else -> "Balanced" },
                        accent = flowAccent
                    )
                }
                if (totalIncome > 0L) {
                    Text("${(spendRatio * 100f).roundToInt()}% of income spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GlowProgressBar(progress = spendRatio, accent = flowAccent, height = 8.dp)
            }
        }

        AccentDivider(accent = MaterialTheme.colorScheme.secondary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryMetricTile(
                title    = "Total Spent",
                amount   = formatAmount(totalExpense),
                accent   = MaterialTheme.colorScheme.error,
                icon     = Icons.AutoMirrored.Filled.TrendingDown,
                chip     = { TrendChip(totalExpense, previousExpense, isExpense = true) },
                modifier = Modifier.weight(1f)
            )
            SummaryMetricTile(
                title    = "Total Income",
                amount   = formatAmount(totalIncome),
                accent   = MaterialTheme.colorScheme.secondary,
                icon     = Icons.AutoMirrored.Filled.TrendingUp,
                chip     = { TrendChip(totalIncome, previousIncome, isExpense = false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricTile(
    title: String,
    amount: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    chip: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.10f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp)) }
                chip()
            }
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
            Text(amount, style = MaterialTheme.typography.headlineMedium.financialFigures(FontWeight.ExtraBold), color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TrendChip(amount: Long, previous: Long, isExpense: Boolean) {
    if (previous <= 0L) {
        val accent = if (amount == 0L) MaterialTheme.colorScheme.outline else if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
        NeonPill(text = if (amount == 0L) "No prior" else if (isExpense) "New spend" else "New income", accent = accent)
        return
    }
    val change     = (amount - previous).toFloat() / previous * 100f
    val isPositive = change > 0f
    val accent     = if (isExpense == isPositive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    val icon       = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else if (abs(change) < 0.1f) Icons.AutoMirrored.Filled.TrendingFlat else Icons.AutoMirrored.Filled.TrendingDown
    val label      = when {
        abs(change) < 0.1f -> "Flat"
        isPositive  -> "+${String.format("%.0f", abs(change))}%"
        else        -> "-${String.format("%.0f", abs(change))}%"
    }
    NeonPill(text = label, accent = accent)
}

@Composable
fun CategoryBreakdownItem(name: String, amount: Long, percentage: Float, color: Color) {
    val accent = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatAmount(amount), style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.Bold))
                    Text("${String.format("%.1f", percentage)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            GlowProgressBar(progress = percentage / 100f, accent = accent, height = 5.dp)
        }
    }
}

@Composable
fun TagBreakdownItem(name: String, colorHex: String, amount: Long, count: Int, percentage: Float) {
    val accent = remember(colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color.Unspecified)
    }
    val resolvedAccent = if (accent == Color.Unspecified) MaterialTheme.colorScheme.tertiary else accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(resolvedAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalOffer, null, tint = resolvedAccent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name.ifEmpty { "Untagged" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$count transactions · ${String.format("%.1f", percentage)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatAmount(amount), style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.ExtraBold), color = resolvedAccent)
            }
            GlowProgressBar(progress = percentage / 100f, accent = resolvedAccent, height = 4.dp)
        }
    }
}

@Composable
private fun RecurringSeriesItem(series: com.expensetracker.app.core.domain.RecurringSeries) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    series.description.ifBlank { "Recurring charge" },
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = "${series.cadence.label} · ${series.occurrences}× · next ${series.nextExpected.format(DateTimeFormatter.ofPattern("MMM d"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text  = formatAmount(series.averageAmountMinor),
                style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun EmptyAnalyticsState(message: String) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
