package com.expensetracker.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.GlowProgressBar
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.adaptiveFlowLayout
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analytics")
                        Text(
                            text = "See where your money is going",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(
                start = ScreenEdgePadding,
                top = 8.dp,
                end = ScreenEdgePadding,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthSelector(
                    currentMonth = uiState.currentMonth,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            item {
                SummaryCard(
                    totalExpense = uiState.totalExpense,
                    totalIncome = uiState.totalIncome,
                    netFlow = uiState.totalIncome - uiState.totalExpense,
                    previousExpense = uiState.previousExpense,
                    previousIncome = uiState.previousIncome
                )
            }

            item {
                Text(
                    text = "By Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.categoryBreakdown.isEmpty()) {
                item {
                    EmptyAnalyticsState(message = "No expense data for this month")
                }
            } else {
                items(uiState.categoryBreakdown) { category ->
                    CategoryBreakdownItem(
                        name = category.categoryName,
                        amount = category.total,
                        percentage = category.percentage,
                        color = category.color
                    )
                }
            }

            item {
                Text(
                    text = "Top Merchants",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.merchantBreakdown.isEmpty()) {
                item {
                    EmptyAnalyticsState(message = "No merchant data available")
                }
            } else {
                items(uiState.merchantBreakdown) { merchant ->
                    MerchantBreakdownItem(
                        name = merchant.merchantName,
                        amount = merchant.total,
                        count = merchant.transactionCount
                    )
                }
            }
        }
    }
}

@Composable
fun MonthSelector(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.primary
    ) {
        SectionHeader(
            eyebrow = "Monthly View",
            title = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            subtitle = "Move month by month to compare totals and trends",
            trailing = {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous"
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next"
                    )
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SummaryCard(
    totalExpense: Long,
    totalIncome: Long,
    netFlow: Long,
    previousExpense: Long,
    previousIncome: Long
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.secondary
    ) {
        SectionHeader(
            eyebrow = "Overview",
            title = "Monthly balance",
            subtitle = "Spend, income, and trend signals at a glance"
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val summaryLayout = adaptiveFlowLayout(
                maxWidth = maxWidth,
                minItemWidth = 152.dp,
                spacing = 10.dp,
                maxColumns = 2
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = summaryLayout.columns
            ) {
                SummaryMetricTile(
                    title = "Total Spent",
                    amount = formatAmount(totalExpense),
                    accent = MaterialTheme.colorScheme.error,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    supporting = if (previousExpense > 0L) "Compared with last month" else "Current month spend",
                    modifier = Modifier.fillMaxWidth(summaryLayout.itemFraction),
                    chip = {
                        TrendChip(
                            amount = totalExpense,
                            previous = previousExpense,
                            isExpense = true
                        )
                    }
                )

                SummaryMetricTile(
                    title = "Total Income",
                    amount = formatAmount(totalIncome),
                    accent = MaterialTheme.colorScheme.secondary,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    supporting = if (previousIncome > 0L) "Compared with last month" else "Current month income",
                    modifier = Modifier.fillMaxWidth(summaryLayout.itemFraction),
                    chip = {
                        TrendChip(
                            amount = totalIncome,
                            previous = previousIncome,
                            isExpense = false
                        )
                    }
                )
            }
        }

        NetFlowHeroCard(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netFlow = netFlow
        )
    }
}

@Composable
private fun SummaryMetricTile(
    title: String,
    amount: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    supporting: String,
    modifier: Modifier = Modifier,
    chip: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.16f)
        ),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.16f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(accent.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        chip()
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = amount,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = supporting,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetFlowHeroCard(
    totalExpense: Long,
    totalIncome: Long,
    netFlow: Long
) {
    val spendRatio = if (totalIncome > 0L) {
        (totalExpense.toFloat() / totalIncome.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val spendRateLabel = if (totalIncome > 0L) {
        "${(spendRatio * 100f).roundToInt()}% of income spent"
    } else if (totalExpense > 0L) {
        "Log income to compare spend rate"
    } else {
        "Add transactions to build a signal"
    }
    val flowAccent = if (netFlow >= 0) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val flowStatus = when {
        netFlow > 0L -> "Positive balance"
        netFlow < 0L -> "Overspending"
        else -> "Balanced"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            flowAccent.copy(alpha = 0.16f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            flowAccent.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Net Flow",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (netFlow >= 0) "+" else "-"}${formatAmount(abs(netFlow))}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = flowAccent
                        )
                    }
                    NetFlowStatusPill(
                        label = flowStatus,
                        accent = flowAccent
                    )
                }
                Text(
                    text = spendRateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlowProgressBar(
                    progress = spendRatio,
                    accent = if (netFlow >= 0) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    height = 12.dp
                )
            }
        }
    }
}

@Composable
private fun NetFlowStatusPill(
    label: String,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }
    }
}

@Composable
fun TrendChip(
    amount: Long,
    previous: Long,
    isExpense: Boolean
) {
    if (previous <= 0L) {
        val accent = when {
            amount == 0L -> MaterialTheme.colorScheme.outline
            isExpense -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.secondary
        }
        val label = when {
            amount == 0L -> "No prior"
            isExpense -> "New spend"
            else -> "New income"
        }

        Surface(
            color = accent.copy(alpha = 0.12f),
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (amount == 0L) Icons.AutoMirrored.Filled.TrendingFlat else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = accent
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
        }
        return
    }

    val change = ((amount - previous).toFloat() / previous * 100f)
    val isPositive = change > 0f
    val accent = if (isExpense == isPositive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val descriptor = when {
        abs(change) < 0.1f -> "Flat"
        isPositive && isExpense -> "Higher"
        isPositive -> "Up"
        isExpense -> "Lower"
        else -> "Down"
    }

    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when {
                    abs(change) < 0.1f -> Icons.AutoMirrored.Filled.TrendingFlat
                    isPositive -> Icons.AutoMirrored.Filled.TrendingUp
                    else -> Icons.AutoMirrored.Filled.TrendingDown
                },
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = accent
            )
            Text(
                text = "${String.format("%.1f", abs(change))}% $descriptor",
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }
    }
}

@Composable
fun CategoryBreakdownItem(
    name: String,
    amount: Long,
    percentage: Float,
    color: Color
) {
    val accent = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = accent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        GlowProgressBar(
            progress = percentage / 100f,
            accent = accent
        )
    }
}

@Composable
fun MerchantBreakdownItem(
    name: String,
    amount: Long,
    count: Int
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$count transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatAmount(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyAnalyticsState(message: String) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
