@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.GlowProgressBar
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.adaptiveFlowLayout
import com.expensetracker.app.ui.theme.financialFigures
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onViewLedger: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenCaptureInbox: () -> Unit,
    onOpenStatementImport: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Expense Tracker")
                        Text(
                            text = "A cleaner view of this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    TextButton(onClick = onViewLedger) {
                        Text("View Ledger")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = ScreenEdgePadding,
                top = 8.dp,
                end = ScreenEdgePadding,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthlySummaryCard(
                    monthName = uiState.monthName,
                    totalExpense = uiState.totalExpense,
                    totalIncome = uiState.totalIncome,
                    budgetRemaining = uiState.budgetRemaining,
                    budgetTotal = uiState.budgetTotal,
                    budgetName = uiState.budgetName,
                    todayExpense = uiState.todayExpense,
                    todayBudgetAllowance = uiState.todayBudgetAllowance,
                    transactionsThisMonth = uiState.transactionsThisMonth,
                    activeDays = uiState.activeDays,
                    streakDays = uiState.streakDays,
                    dayOfMonth = uiState.dayOfMonth,
                    daysInMonth = uiState.daysInMonth,
                    netFlow = uiState.netFlow
                )
            }

            item {
                QuickActionGrid(
                    budgetName = uiState.budgetName,
                    openCaptureCount = uiState.openCaptureCount,
                    onAddTransaction = onAddTransaction,
                    onOpenBudget = onOpenBudget,
                    onOpenCaptureInbox = onOpenCaptureInbox,
                    onOpenStatementImport = onOpenStatementImport
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Latest activity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onViewLedger) {
                        Text("View all")
                    }
                }
            }

            if (uiState.recentTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "No transactions yet",
                        action = "Add a transaction to get started"
                    )
                }
            } else {
                items(uiState.recentTransactions) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlySummaryCard(
    monthName: String,
    totalExpense: Long,
    totalIncome: Long,
    budgetRemaining: Long?,
    budgetTotal: Long?,
    budgetName: String?,
    todayExpense: Long,
    todayBudgetAllowance: Long?,
    transactionsThisMonth: Int,
    activeDays: Int,
    streakDays: Int,
    dayOfMonth: Int,
    daysInMonth: Int,
    netFlow: Long
) {
    val coveragePercent = if (dayOfMonth > 0) {
        ((activeDays.toFloat() / dayOfMonth.toFloat()) * 100f).roundToInt()
    } else {
        0
    }
    val paceValue = when {
        todayBudgetAllowance == null || budgetTotal == null -> "No budget"
        todayExpense == 0L -> "Light"
        todayExpense > todayBudgetAllowance -> "Fast"
        else -> "On track"
    }
    val paceSupporting = when {
        todayBudgetAllowance == null || budgetTotal == null -> "Add a budget for day caps"
        todayExpense == 0L -> "${formatAmount(todayBudgetAllowance)} available today"
        todayExpense > todayBudgetAllowance -> "${formatAmount(todayExpense - todayBudgetAllowance)} over today"
        else -> "${formatAmount(todayBudgetAllowance - todayExpense)} left today"
    }
    val paceAccent = when {
        todayBudgetAllowance == null || budgetTotal == null -> MaterialTheme.colorScheme.outline
        todayExpense > todayBudgetAllowance -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.primary
    ) {
        SectionHeader(
            eyebrow = "This Month",
            title = monthName,
            subtitle = "$transactionsThisMonth entries across $activeDays active days",
            trailing = {
                CurrencyBadge(code = "INR")
            }
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val statLayout = adaptiveFlowLayout(
                maxWidth = maxWidth,
                minItemWidth = 148.dp,
                spacing = 10.dp,
                maxColumns = 2
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = statLayout.columns
            ) {
                InsightStatCard(
                    label = "Streak",
                    value = "${streakDays}d",
                    supporting = if (streakDays > 0) "Current logging run" else "Start your run",
                    icon = Icons.Default.LocalFireDepartment,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth(statLayout.itemFraction)
                        .heightIn(min = 118.dp)
                )
                InsightStatCard(
                    label = "Coverage",
                    value = "$coveragePercent%",
                    supporting = "$activeDays of $dayOfMonth days",
                    icon = Icons.Default.CalendarToday,
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth(statLayout.itemFraction)
                        .heightIn(min = 118.dp)
                )
            }
        }

        PaceHighlightCard(
            paceValue = paceValue,
            paceSupporting = paceSupporting,
            accent = paceAccent
        )

        BalanceSnapshotCard(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netFlow = netFlow
        )

        if (budgetRemaining != null && budgetTotal != null && budgetTotal > 0 && todayBudgetAllowance != null) {
            BudgetProgressSection(
                totalExpense = totalExpense,
                budgetTotal = budgetTotal,
                budgetRemaining = budgetRemaining,
                budgetName = budgetName,
                todayExpense = todayExpense,
                todayBudgetAllowance = todayBudgetAllowance,
                dayOfMonth = dayOfMonth,
                daysInMonth = daysInMonth
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                )
            ) {
                Text(
                    text = "Set a monthly budget from home to unlock month and per-day pacing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun CurrencyBadge(
    code: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u20B9",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = code,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun InsightStatCard(
    label: String,
    value: String,
    supporting: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 126.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PaceHighlightCard(
    paceValue: String,
    paceSupporting: String,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = accent
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pace",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = paceValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = paceSupporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BalanceSnapshotCard(
    totalExpense: Long,
    totalIncome: Long,
    netFlow: Long
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val balanceLayout = adaptiveFlowLayout(
            maxWidth = maxWidth,
            minItemWidth = 148.dp,
            spacing = 10.dp,
            maxColumns = 2
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = balanceLayout.columns
        ) {
            BalanceSnapshotItem(
                label = "Spent",
                amount = formatAmount(totalExpense),
                tint = MaterialTheme.colorScheme.error,
                icon = Icons.Default.ArrowDownward,
                modifier = Modifier.fillMaxWidth(balanceLayout.itemFraction)
            )
            BalanceSnapshotItem(
                label = "Received",
                amount = formatAmount(totalIncome),
                tint = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.ArrowUpward,
                modifier = Modifier.fillMaxWidth(balanceLayout.itemFraction)
            )
            BalanceSnapshotItem(
                label = "Net flow",
                amount = "${if (netFlow >= 0) "+" else "-"}${formatAmount(abs(netFlow))}",
                tint = if (netFlow >= 0) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.error
                },
                icon = Icons.Default.AutoAwesome,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BalanceSnapshotItem(
    label: String,
    amount: String,
    tint: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(tint.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge.financialFigures(FontWeight.ExtraBold),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BudgetProgressSection(
    totalExpense: Long,
    budgetTotal: Long,
    budgetRemaining: Long,
    budgetName: String?,
    todayExpense: Long,
    todayBudgetAllowance: Long,
    dayOfMonth: Int,
    daysInMonth: Int
) {
    val todayRemaining = todayBudgetAllowance - todayExpense
    val monthHeadline = if (budgetRemaining >= 0) {
        formatAmount(budgetRemaining)
    } else {
        formatAmount(abs(budgetRemaining))
    }
    val todayHeadline = if (todayRemaining >= 0) {
        formatAmount(todayRemaining)
    } else {
        formatAmount(abs(todayRemaining))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Budget pace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Month and per-day guidance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${formatAverageAmount(budgetTotal, daysInMonth)} avg/day",
                    style = MaterialTheme.typography.labelLarge.financialFigures(FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val budgetLayout = adaptiveFlowLayout(
                maxWidth = maxWidth,
                minItemWidth = 152.dp,
                spacing = 10.dp,
                maxColumns = 2
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = budgetLayout.columns
            ) {
                BudgetPaceCard(
                    label = budgetName ?: "Month budget",
                    headline = monthHeadline,
                    status = if (budgetRemaining >= 0) "Left" else "Over",
                    supporting = "${formatAmount(totalExpense)} spent of ${formatAmount(budgetTotal)}",
                    progress = (totalExpense.toFloat() / budgetTotal.toFloat()).coerceIn(0f, 1f),
                    accent = if (budgetRemaining < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.fillMaxWidth(budgetLayout.itemFraction)
                )
                BudgetPaceCard(
                    label = "Daily allowance",
                    headline = todayHeadline,
                    status = if (todayRemaining >= 0) "Left today" else "Over today",
                    supporting = "${formatAmount(todayExpense)} spent on day $dayOfMonth/$daysInMonth of ${formatAmount(todayBudgetAllowance)}",
                    progress = if (todayBudgetAllowance > 0) {
                        (todayExpense.toFloat() / todayBudgetAllowance.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    accent = if (todayRemaining < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.fillMaxWidth(budgetLayout.itemFraction)
                )
            }
        }
    }
}

@Composable
private fun BudgetPaceCard(
    label: String,
    headline: String,
    status: String,
    supporting: String,
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium.financialFigures(FontWeight.ExtraBold),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            GlowProgressBar(
                progress = progress,
                accent = accent
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionGrid(
    budgetName: String?,
    openCaptureCount: Int,
    onAddTransaction: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenCaptureInbox: () -> Unit,
    onOpenStatementImport: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.secondary
    ) {
        SectionHeader(
            eyebrow = "Shortcuts",
            title = "Move faster from home",
            subtitle = "Quick access to tracking, budgets, and smart capture."
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val actionLayout = adaptiveFlowLayout(
                maxWidth = maxWidth,
                minItemWidth = 156.dp,
                spacing = 10.dp,
                maxColumns = 2
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = actionLayout.columns
            ) {
                HomeActionCard(
                    title = "Add transaction",
                    subtitle = "Log cash, card, or UPI activity",
                    icon = Icons.Default.Add,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(actionLayout.itemFraction),
                    testTag = "home_action_add",
                    onClick = onAddTransaction
                )
                HomeActionCard(
                    title = budgetName ?: "Set Budget",
                    subtitle = if (budgetName == null) "Create a monthly limit" else "Adjust monthly target",
                    icon = Icons.Default.Savings,
                    accent = MaterialTheme.colorScheme.secondary,
                    badge = if (budgetName == null) "Plan" else "Active",
                    modifier = Modifier.fillMaxWidth(actionLayout.itemFraction),
                    testTag = "home_action_budget",
                    onClick = onOpenBudget
                )
                HomeActionCard(
                    title = if (openCaptureCount > 0) "Capture Inbox" else "Smart Capture",
                    subtitle = if (openCaptureCount > 0) {
                        "$openCaptureCount payment${if (openCaptureCount == 1) "" else "s"} waiting"
                    } else {
                        "Sync SMS and notification detections"
                    },
                    icon = Icons.Default.AutoAwesome,
                    accent = MaterialTheme.colorScheme.tertiary,
                    badge = if (openCaptureCount > 0) "$openCaptureCount waiting" else "Live",
                    modifier = Modifier.fillMaxWidth(actionLayout.itemFraction),
                    testTag = "home_action_capture",
                    onClick = onOpenCaptureInbox
                )
                HomeActionCard(
                    title = "Statements",
                    subtitle = "Import bank or card statements",
                    icon = Icons.Default.Receipt,
                    accent = MaterialTheme.colorScheme.primary,
                    badge = "PDF",
                    modifier = Modifier.fillMaxWidth(actionLayout.itemFraction),
                    testTag = "home_action_statement",
                    onClick = onOpenStatementImport
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    badge: String? = null,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 146.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.12f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.46f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(accent.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent
                        )
                    }
                    badge?.let {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isIncome = transaction.type.name == "INCOME"
    val accent = if (isIncome) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val amountTint = if (isIncome) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val icon = if (isIncome) {
        Icons.Default.Add
    } else {
        Icons.Default.Receipt
    }
    val primaryLabel = transaction.description ?: transaction.notes ?: "Expense"
    val metaLine = buildString {
        append(
            transaction.transactionTime.format(
                DateTimeFormatter.ofPattern("dd MMM | HH:mm")
            )
        )
        transaction.paymentMethod?.takeIf { it.isNotBlank() }?.let {
            append("  |  ")
            append(it)
        }
        val sourceLabel = transaction.source.name
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }
        append("  |  ")
        append(sourceLabel)
    }
    val secondaryLabel = transaction.notes
        ?.takeIf { it.isNotBlank() && it != primaryLabel }
        ?: transaction.description
            ?.takeIf { it.isNotBlank() && it != primaryLabel }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.14f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    secondaryLabel?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = amountTint.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isIncome) "In" else "Out",
                            style = MaterialTheme.typography.labelMedium,
                            color = amountTint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Text(
                        text = "${if (isIncome) "+" else "-"}${formatAmount(transaction.amountMinor)}",
                        style = MaterialTheme.typography.titleMedium.financialFigures(FontWeight.ExtraBold),
                        fontWeight = FontWeight.ExtraBold,
                        color = amountTint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    action: String
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = action,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatAmount(amountMinor: Long): String {
    val rupees = amountMinor / 100
    val paise = amountMinor % 100
    return "\u20B9$rupees${if (paise > 0) ".${paise.toString().padStart(2, '0')}" else ""}"
}

private fun formatAverageAmount(amountMinor: Long, divisor: Int): String {
    if (divisor <= 0) {
        return formatAmount(amountMinor)
    }
    val amount = amountMinor.toDouble() / divisor.toDouble() / 100.0
    return String.format(Locale.ENGLISH, "\u20B9%.2f", amount)
}
