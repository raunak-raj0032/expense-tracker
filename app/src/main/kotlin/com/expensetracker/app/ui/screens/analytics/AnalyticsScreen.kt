package com.expensetracker.app.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.AccentDivider
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.GlowProgressBar
import com.expensetracker.app.ui.theme.NeonPill
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.CardSpacing
import com.expensetracker.app.ui.theme.SectionSpacing
import com.expensetracker.app.ui.theme.financialFigures
import kotlinx.coroutines.delay
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); visible = true }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                            Text("Money Story", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("Trends & insights at a glance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(
                start = ScreenEdgePadding, top = CardSpacing,
                end   = ScreenEdgePadding, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing), initialOffsetY = { -20 })) {
                    MonthSelector(uiState.currentMonth, viewModel::previousMonth, viewModel::nextMonth)
                }
            }
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(450, 80)) + slideInVertically(animationSpec = tween(450, 80, FastOutSlowInEasing), initialOffsetY = { 24 })) {
                    SummaryCard(uiState.totalExpense, uiState.totalIncome, uiState.totalIncome - uiState.totalExpense, uiState.previousExpense, uiState.previousIncome)
                }
            }
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(450, 120))) {
                    AiInsightCard(
                        insight = uiState.aiInsight,
                        error = uiState.aiInsightError,
                        loading = uiState.aiInsightLoading,
                        onGenerate = viewModel::generateAiInsights
                    )
                }
            }

            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 160))) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.width(3.dp).height(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Column {
                            Text("CATEGORY PULSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp)
                            Text("Where your money goes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (uiState.categoryBreakdown.isEmpty()) {
                item { EmptyAnalyticsState("No expense data for this month") }
            } else {
                items(uiState.categoryBreakdown) { category ->
                    CategoryBreakdownItem(category.categoryName, category.total, category.percentage, category.color)
                }
            }

            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 200))) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.width(3.dp).height(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                        Column {
                            Text("TOP PLACES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, letterSpacing = 1.2.sp)
                            Text("Most visited merchants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (uiState.merchantBreakdown.isEmpty()) {
                item { EmptyAnalyticsState("No merchant data available") }
            } else {
                items(uiState.merchantBreakdown) { merchant ->
                    MerchantBreakdownItem(merchant.merchantName, merchant.total, merchant.transactionCount)
                }
            }

            if (uiState.recurringSeries.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 240))) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                            Column {
                                Text("SUBSCRIPTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.2.sp)
                                Text("Recurring charges we spotted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                items(uiState.recurringSeries) { series ->
                    RecurringSeriesItem(series)
                }
            }
        }
    }
}

@Composable
private fun AiInsightCard(
    insight: String?,
    error: String?,
    loading: Boolean,
    onGenerate: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                eyebrow = "AI Insights",
                title = "AI read on your month",
                subtitle = "Uses your configured AI provider"
            )
            when {
                loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Asking llama3.2...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                error != null -> Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                insight != null -> Text(insight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                else -> Text("Generate a short spending analysis from this month's totals, categories, merchants, and recurring charges.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onGenerate,
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (insight == null) "Generate insights" else "Refresh insights")
            }
        }
    }
}

@Composable
private fun RecurringSeriesItem(series: com.expensetracker.app.core.domain.RecurringSeries) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(series.description.ifBlank { "Recurring charge" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${series.cadence.label} · ${series.occurrences}\u00D7 · next ${series.nextExpected.format(DateTimeFormatter.ofPattern("MMM d"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatAmount(series.averageAmountMinor),
                style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun MonthSelector(currentMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        SectionHeader(
            eyebrow  = "Monthly View",
            title    = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            subtitle = "Swipe months to compare",
            trailing = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous", tint = MaterialTheme.colorScheme.primary)
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).align(Alignment.CenterVertically).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
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

        // Big net-flow display
        val flowAccent = if (netFlow >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val spendRatio = if (totalIncome > 0L) (totalExpense.toFloat() / totalIncome.toFloat()).coerceIn(0f, 1f) else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Brush.linearGradient(colors = listOf(flowAccent.copy(alpha = 0.12f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))))
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

        // Spent / Income tiles
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 320.dp) {
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
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryMetricTile(
                        title    = "Total Spent",
                        amount   = formatAmount(totalExpense),
                        accent   = MaterialTheme.colorScheme.error,
                        icon     = Icons.AutoMirrored.Filled.TrendingDown,
                        chip     = { TrendChip(totalExpense, previousExpense, isExpense = true) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SummaryMetricTile(
                        title    = "Total Income",
                        amount   = formatAmount(totalIncome),
                        accent   = MaterialTheme.colorScheme.secondary,
                        icon     = Icons.AutoMirrored.Filled.TrendingUp,
                        chip     = { TrendChip(totalIncome, previousIncome, isExpense = false) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
            .background(Brush.linearGradient(colors = listOf(accent.copy(alpha = 0.10f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp)) }
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
        val accent = when { amount == 0L -> MaterialTheme.colorScheme.outline; isExpense -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.secondary }
        NeonPill(text = if (amount == 0L) "No prior" else if (isExpense) "New spend" else "New income", accent = accent)
        return
    }
    val change     = ((amount - previous).toFloat() / previous * 100f)
    val isPositive = change > 0f
    val accent     = if (isExpense == isPositive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    val label      = when {
        abs(change) < 0.1f -> "Flat"
        isPositive && isExpense -> "+${String.format("%.0f", abs(change))}% Higher"
        isPositive -> "+${String.format("%.0f", abs(change))}% Up"
        isExpense  -> "-${String.format("%.0f", abs(change))}% Lower"
        else       -> "-${String.format("%.0f", abs(change))}% Down"
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
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accent))
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
fun MerchantBreakdownItem(name: String, amount: Long, count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name.ifEmpty { "Unknown" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatAmount(amount), style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.ExtraBold), color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun EmptyAnalyticsState(message: String) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
