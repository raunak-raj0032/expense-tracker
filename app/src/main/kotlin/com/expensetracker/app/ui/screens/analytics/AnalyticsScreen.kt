package com.expensetracker.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.GlowProgressBar
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.11f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total Spent", style = MaterialTheme.typography.bodySmall)
                    Text(
                        formatAmount(totalExpense),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    TrendChip(
                        amount = totalExpense,
                        previous = previousExpense,
                        isExpense = true
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.11f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total Income", style = MaterialTheme.typography.bodySmall)
                    Text(
                        formatAmount(totalIncome),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    TrendChip(
                        amount = totalIncome,
                        previous = previousIncome,
                        isExpense = false
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Net Flow",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (netFlow >= 0) "+" else "-"}${formatAmount(abs(netFlow))}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (netFlow >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                GlowProgressBar(
                    progress = if (totalIncome > 0L) {
                        (totalExpense.toFloat() / totalIncome.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    accent = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun TrendChip(
    amount: Long,
    previous: Long,
    isExpense: Boolean
) {
    val change = if (previous > 0) ((amount - previous).toFloat() / previous * 100) else 0f
    val isPositive = change > 0
    val accent = if (isExpense == isPositive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = accent
        )
        Text(
            text = "${String.format("%.1f", abs(change))}%",
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
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
