package com.expensetracker.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.GlowProgressBar
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.StatBadge
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onViewLedger: () -> Unit,
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
                    transactionsThisMonth = uiState.transactionsThisMonth,
                    activeDays = uiState.activeDays,
                    streakDays = uiState.streakDays,
                    momentumScore = uiState.momentumScore,
                    focusRank = uiState.focusRank,
                    missionProgress = uiState.missionProgress,
                    missionLabel = uiState.missionLabel,
                    netFlow = uiState.netFlow
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
                        onClick = { }
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
    transactionsThisMonth: Int,
    activeDays: Int,
    streakDays: Int,
    momentumScore: Int,
    focusRank: String,
    missionProgress: Float,
    missionLabel: String,
    netFlow: Long
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.primary
    ) {
        SectionHeader(
            eyebrow = "This Month",
            title = monthName,
            subtitle = "$transactionsThisMonth entries logged"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBadge(
                label = "Score",
                value = momentumScore.toString(),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.primary
            )
            StatBadge(
                label = "Level",
                value = focusRank,
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary
            )
            StatBadge(
                label = "Active",
                value = "$activeDays days",
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.tertiary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                        )
                    ),
                    shape = MaterialTheme.shapes.small
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatAmount(totalExpense),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Net",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${if (netFlow >= 0) "+" else "-"}${formatAmount(abs(netFlow))}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (netFlow >= 0) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Received",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatAmount(totalIncome),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$transactionsThisMonth / 12 entries",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            GlowProgressBar(
                progress = missionProgress,
                accent = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = missionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (budgetRemaining != null && budgetTotal != null && budgetTotal > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Budget Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                GlowProgressBar(
                    progress = ((budgetTotal - budgetRemaining).toFloat() / budgetTotal).coerceIn(0f, 1f),
                    accent = if (budgetRemaining < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                )
                Text(
                    text = "Budget: ${formatAmount(budgetRemaining)} remaining of ${formatAmount(budgetTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (streakDays > 0) {
            Text(
                text = "Current streak: $streakDays day${if (streakDays == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val accent = if (transaction.type.name == "INCOME") {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val icon = if (transaction.type.name == "INCOME") {
        Icons.Default.Add
    } else {
        Icons.Default.Receipt
    }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        accent = accent,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                    text = transaction.description ?: transaction.notes ?: "Expense",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.transactionTime.format(
                            DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    transaction.paymentMethod?.let { paymentMethod ->
                        Text(
                            text = paymentMethod,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.type.name == "INCOME") "+" else "-"}${formatAmount(transaction.amountMinor)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (transaction.type.name == "INCOME") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = transaction.source.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
