@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.expensetracker.app.auth.AuthState
import com.expensetracker.app.auth.AuthViewModel
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.ui.mascot.TutorialTarget
import com.expensetracker.app.ui.theme.AccentDivider
import com.expensetracker.app.ui.theme.MainTopBar
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.GlowProgressBar
import com.expensetracker.app.ui.theme.NeonPill
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.CardSpacing
import com.expensetracker.app.ui.theme.SectionSpacing
import com.expensetracker.app.ui.theme.adaptiveFlowLayout
import com.expensetracker.app.ui.theme.financialFigures
import kotlinx.coroutines.delay
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
    onOpenProfile: () -> Unit = {},
    onTutorialTargetPositioned: (TutorialTarget, Rect) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val firstName = (authState as? AuthState.SignedIn)?.user?.firstName.orEmpty()
    val profilePicturePath = uiState.profilePicturePath
    var visible by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (uiState.selectionMode) {
                MainTopBar(
                    title  = "${uiState.selectedTransactionIds.size} selected",
                    accent = MaterialTheme.colorScheme.error
                ) {
                    IconButton(onClick = viewModel::clearSelection) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                    IconButton(onClick = { showBulkDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete selected", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                MainTopBar(
                    title    = if (firstName.isNotBlank()) "Hi, $firstName" else "Pocket Pulse",
                    subtitle = if (firstName.isNotBlank()) "Welcome back" else "Your daily money rhythm",
                    accent   = MaterialTheme.colorScheme.primary
                ) {
                    TextButton(onClick = onViewLedger) {
                        Text("Ledger", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (profilePicturePath.isNotEmpty() && java.io.File(profilePicturePath).exists())
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            .clickable { onOpenProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePicturePath.isNotEmpty() && java.io.File(profilePicturePath).exists()) {
                            AsyncImage(
                                model = java.io.File(profilePicturePath),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, "Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onAddTransaction,
                modifier = Modifier.onGloballyPositioned {
                    onTutorialTargetPositioned(TutorialTarget.AddButton, it.boundsInRoot())
                },
                containerColor   = MaterialTheme.colorScheme.primary,
                contentColor     = MaterialTheme.colorScheme.onPrimary,
                elevation        = FloatingActionButtonDefaults.elevation(8.dp, 12.dp),
                shape            = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(26.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start  = ScreenEdgePadding,
                top    = CardSpacing,
                end    = ScreenEdgePadding,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(400)) + slideInVertically(
                        initialOffsetY = { -30 },
                        animationSpec  = tween(400, easing = FastOutSlowInEasing)
                    )
                ) {
                    MonthlySummaryCard(
                        modifier = Modifier.onGloballyPositioned {
                            onTutorialTargetPositioned(TutorialTarget.MonthlySummary, it.boundsInRoot())
                        },
                        monthName            = uiState.monthName,
                        totalExpense         = uiState.totalExpense,
                        totalIncome          = uiState.totalIncome,
                        budgetRemaining      = uiState.budgetRemaining,
                        budgetTotal          = uiState.budgetTotal,
                        budgetName           = uiState.budgetName,
                        todayExpense         = uiState.todayExpense,
                        todayBudgetAllowance = uiState.todayBudgetAllowance,
                        transactionsThisMonth= uiState.transactionsThisMonth,
                        activeDays           = uiState.activeDays,
                        streakDays           = uiState.streakDays,
                        dayOfMonth           = uiState.dayOfMonth,
                        daysInMonth          = uiState.daysInMonth,
                        netFlow              = uiState.netFlow
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(500, delayMillis = 80)) + slideInVertically(
                        initialOffsetY = { 24 },
                        animationSpec  = tween(500, delayMillis = 80, easing = FastOutSlowInEasing)
                    )
                ) {
                    QuickActionGrid(
                        budgetName          = uiState.budgetName,
                        openCaptureCount    = uiState.openCaptureCount,
                        onAddTransaction    = onAddTransaction,
                        onOpenBudget        = onOpenBudget,
                        onOpenCaptureInbox  = onOpenCaptureInbox,
                        onOpenStatementImport = onOpenStatementImport,
                        modifier = Modifier.onGloballyPositioned {
                            onTutorialTargetPositioned(TutorialTarget.QuickActions, it.boundsInRoot())
                        }
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(500, delayMillis = 140))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text  = "RECENT ACTIVITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text       = "Latest transactions",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onViewLedger) {
                            Text(
                                text  = "View all",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (uiState.recentTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "Hmm, my belly's empty.",
                        action  = "Tap the + button to feed me your first transaction!"
                    )
                }
            } else {
                itemsIndexed(uiState.recentTransactions) { index, transaction ->
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn(tween(400, delayMillis = 160 + index * 40)) +
                                  slideInVertically(
                                      initialOffsetY = { 20 },
                                      animationSpec  = tween(400, delayMillis = 160 + index * 40, easing = FastOutSlowInEasing)
                                  )
                    ) {
                        val isSelected = transaction.id in uiState.selectedTransactionIds
                        TransactionListItem(
                            transaction = transaction,
                            onClick = {
                                if (uiState.selectionMode) viewModel.toggleSelection(transaction.id)
                                else onTransactionClick(transaction.id)
                            },
                            selected = isSelected,
                            onLongClick = { viewModel.toggleSelection(transaction.id) }
                        )
                    }
                }
            }
        }
    }

    if (showBulkDeleteDialog) {
        val count = uiState.selectedTransactionIds.size
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Delete $count transaction${if (count == 1) "" else "s"}?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Selected entries will be removed from your ledger.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showBulkDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) { Text("Cancel") }
            }
        )
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
    netFlow: Long,
    modifier: Modifier = Modifier
) {
    val coveragePercent = if (dayOfMonth > 0)
        ((activeDays.toFloat() / dayOfMonth.toFloat()) * 100f).roundToInt() else 0

    val paceAccent = when {
        todayBudgetAllowance == null || budgetTotal == null -> MaterialTheme.colorScheme.onSurfaceVariant
        todayExpense > todayBudgetAllowance -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val paceValue = when {
        todayBudgetAllowance == null || budgetTotal == null -> "No budget"
        todayExpense == 0L -> "Cruising"
        todayExpense > todayBudgetAllowance -> "Too fast"
        else -> "Steady"
    }
    
    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        accent   = MaterialTheme.colorScheme.primary
    ) {
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text  = "$monthName · Day $dayOfMonth".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
            }
            NeonPill(text = "INR ₹")
        }

        AccentDivider(accent = MaterialTheme.colorScheme.primary)

        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text  = "NET FLOW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.0.sp
            )
            Text(
                text  = "${if (netFlow >= 0) "+" else ""}${formatAmount(abs(netFlow))}",
                style = MaterialTheme.typography.displayMedium.financialFigures(FontWeight.Black),
                color = if (netFlow >= 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
            )
            Text(
                text  = "$transactionsThisMonth transactions · $activeDays active days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BalanceChip(
                label  = "Spent",
                amount = formatAmount(totalExpense),
                tint   = MaterialTheme.colorScheme.error,
                icon   = Icons.Default.ArrowDownward,
                modifier = Modifier.weight(1f)
            )
            BalanceChip(
                label  = "Received",
                amount = formatAmount(totalIncome),
                tint   = MaterialTheme.colorScheme.secondary,
                icon   = Icons.Default.ArrowUpward,
                modifier = Modifier.weight(1f)
            )
            BalanceChip(
                label  = "Pace",
                amount = paceValue,
                tint   = paceAccent,
                icon   = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
        }

        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InsightStatCard(
                label     = "Streak",
                value     = "${streakDays}d",
                supporting= if (streakDays > 0) "Keep it going" else "Start tracking",
                icon      = Icons.Default.LocalFireDepartment,
                accent    = MaterialTheme.colorScheme.tertiary,
                modifier  = Modifier.weight(1f).heightIn(min = 110.dp)
            )
            InsightStatCard(
                label     = "Coverage",
                value     = "$coveragePercent%",
                supporting= "$activeDays of $dayOfMonth days",
                icon      = Icons.Default.CalendarToday,
                accent    = MaterialTheme.colorScheme.secondary,
                modifier  = Modifier.weight(1f).heightIn(min = 110.dp)
            )
        }

        
        if (budgetRemaining != null && budgetTotal != null && budgetTotal > 0 && todayBudgetAllowance != null) {
            BudgetProgressSection(
                totalExpense         = totalExpense,
                budgetTotal          = budgetTotal,
                budgetRemaining      = budgetRemaining,
                budgetName           = budgetName,
                todayExpense         = todayExpense,
                todayBudgetAllowance = todayBudgetAllowance,
                dayOfMonth           = dayOfMonth,
                daysInMonth          = daysInMonth
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text  = "Set a monthly budget to unlock day-by-day pacing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceChip(
    label: String,
    amount: String,
    tint: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(tint.copy(alpha = 0.10f))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint
                )
            }
            Text(
                text       = amount,
                style      = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.Bold),
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
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
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleLarge.financialFigures(FontWeight.ExtraBold), maxLines = 1)
            Text(text = supporting, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
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
    var viewMode by remember { mutableStateOf(BudgetViewMode.MONTHLY) }

    val weeklyBudget = (budgetTotal / 4)
    val currentWeek = ((dayOfMonth - 1) / 7) + 1
    val daysIntoWeek = ((dayOfMonth - 1) % 7) + 1
    val weeklySpent = (totalExpense * 7 / daysInMonth)
    val weeklyAllowance = todayBudgetAllowance * 7

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("BUDGET PACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.0.sp)
                Text("Month & daily guidance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = if (viewMode == BudgetViewMode.MONTHLY) "Monthly" else "Weekly",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { viewMode = if (viewMode == BudgetViewMode.MONTHLY) BudgetViewMode.WEEKLY else BudgetViewMode.MONTHLY },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Toggle view",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val layout = adaptiveFlowLayout(maxWidth, 152.dp, 8.dp, 2)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow     = layout.columns
            ) {
                if (viewMode == BudgetViewMode.MONTHLY) {
                    BudgetPaceCard(
                        label      = budgetName ?: "Month budget",
                        headline   = formatAmount(abs(budgetRemaining)),
                        status     = if (budgetRemaining >= 0) "Remaining" else "Over",
                        supporting = "${formatAmount(totalExpense)} of ${formatAmount(budgetTotal)} spent",
                        progress   = (totalExpense.toFloat() / budgetTotal.toFloat()).coerceIn(0f, 1f),
                        accent     = if (budgetRemaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        modifier   = Modifier.fillMaxWidth(layout.itemFraction)
                    )
                    BudgetPaceCard(
                        label      = "Daily allowance",
                        headline   = formatAmount(abs(todayBudgetAllowance - todayExpense)),
                        status     = if (todayBudgetAllowance - todayExpense >= 0) "Left today" else "Over",
                        supporting = "Day $dayOfMonth/$daysInMonth · ${formatAmount(todayExpense)} spent",
                        progress   = if (todayBudgetAllowance > 0)
                            (todayExpense.toFloat() / todayBudgetAllowance.toFloat()).coerceIn(0f, 1f) else 0f,
                        accent     = if (todayBudgetAllowance - todayExpense < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier   = Modifier.fillMaxWidth(layout.itemFraction)
                    )
                } else {
                    BudgetPaceCard(
                        label      = "Week $currentWeek",
                        headline   = formatAmount(abs(weeklyBudget - weeklySpent)),
                        status     = if (weeklyBudget >= weeklySpent) "Remaining" else "Over",
                        supporting = "${formatAmount(weeklySpent)} of ${formatAmount(weeklyBudget)} spent",
                        progress   = if (weeklyBudget > 0) (weeklySpent.toFloat() / weeklyBudget.toFloat()).coerceIn(0f, 1f) else 0f,
                        accent     = if (weeklyBudget < weeklySpent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        modifier   = Modifier.fillMaxWidth(layout.itemFraction)
                    )
                    BudgetPaceCard(
                        label      = "Daily this week",
                        headline   = formatAmount(abs(weeklyAllowance / 7 - todayExpense)),
                        status     = if (weeklyAllowance / 7 >= todayExpense) "Left today" else "Over",
                        supporting = "Day $daysIntoWeek of week · ${formatAmount(todayExpense)} spent",
                        progress   = if (weeklyAllowance > 0) (todayExpense * 7.toFloat() / weeklyAllowance.toFloat()).coerceIn(0f, 1f) else 0f,
                        accent     = if (weeklyAllowance / 7 < todayExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier   = Modifier.fillMaxWidth(layout.itemFraction)
                    )
                }
            }
        }
    }
}

private enum class BudgetViewMode { MONTHLY, WEEKLY }

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
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                )
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                NeonPill(text = status, accent = accent)
            }
            Text(
                text     = headline,
                style    = MaterialTheme.typography.headlineMedium.financialFigures(FontWeight.ExtraBold),
                color    = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            GlowProgressBar(progress = progress, accent = accent)
            Text(supporting, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onOpenStatementImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        accent   = MaterialTheme.colorScheme.secondary
    ) {
        SectionHeader(
            eyebrow  = "Shortcuts",
            title    = "Fast Lane",
            subtitle = "Actions you need every day"
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeActionCard(
                    title    = "Add transaction",
                    subtitle = "Log cash, card, UPI",
                    icon     = Icons.Default.Add,
                    accent   = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    testTag  = "home_action_add",
                    onClick  = onAddTransaction
                )
                HomeActionCard(
                    title    = budgetName ?: "Set Budget",
                    subtitle = if (budgetName == null) "Create monthly limit" else "Adjust target",
                    icon     = Icons.Default.Savings,
                    accent   = MaterialTheme.colorScheme.secondary,
                    badge    = if (budgetName == null) "Plan" else "Active",
                    modifier = Modifier.weight(1f),
                    testTag  = "home_action_budget",
                    onClick  = onOpenBudget
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeActionCard(
                    title    = if (openCaptureCount > 0) "Capture Inbox" else "Smart Capture",
                    subtitle = if (openCaptureCount > 0) "$openCaptureCount waiting" else "SMS & notifications",
                    icon     = Icons.Default.AutoAwesome,
                    accent   = MaterialTheme.colorScheme.tertiary,
                    badge    = if (openCaptureCount > 0) "$openCaptureCount" else "Live",
                    modifier = Modifier.weight(1f),
                    testTag  = "home_action_capture",
                    onClick  = onOpenCaptureInbox
                )
                HomeActionCard(
                    title    = "Statements",
                    subtitle = "Import bank/card PDFs",
                    icon     = Icons.Default.Receipt,
                    accent   = MaterialTheme.colorScheme.primary,
                    badge    = "PDF",
                    modifier = Modifier.weight(1f),
                    testTag  = "home_action_statement",
                    onClick  = onOpenStatementImport
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    badge: String? = null,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(144.dp)
            .testTag(testTag)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                badge?.let { NeonPill(text = it, accent = accent) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val isIncome  = transaction.type.name == "INCOME"
    val accent    = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    val amountTint= if (isIncome) MaterialTheme.colorScheme.primary   else MaterialTheme.colorScheme.error
    val icon      = if (isIncome) Icons.Default.ArrowUpward           else Icons.Default.ArrowDownward

    val primaryLabel  = transaction.description ?: transaction.notes ?: "Expense"
    val secondaryLabel= transaction.notes?.takeIf { it.isNotBlank() && it != primaryLabel }
        ?: transaction.description?.takeIf { it.isNotBlank() && it != primaryLabel }
    val metaLine = buildString {
        append(transaction.transactionTime.format(DateTimeFormatter.ofPattern("dd MMM · HH:mm")))
        transaction.paymentMethod?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
    }

    val containerColor = if (selected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    else
        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(60.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(amountTint)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(primaryLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(metaLine, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                secondaryLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val homeCurrency = com.expensetracker.app.ui.money.LocalHomeCurrency.current
                val sign = if (isIncome) "+" else "-"
                val nativeText = "$sign${com.expensetracker.app.core.money.CurrencyConverter.formatAmount(transaction.amountMinor, transaction.currencyCode)}"
                Text(
                    text  = nativeText,
                    style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.ExtraBold),
                    color = amountTint
                )
                if (!transaction.currencyCode.equals(homeCurrency, ignoreCase = true)) {
                    val converted = com.expensetracker.app.core.money.CurrencyConverter.convert(
                        transaction.amountMinor, transaction.currencyCode, homeCurrency
                    )
                    Text(
                        text = "\u2248 ${com.expensetracker.app.core.money.CurrencyConverter.formatAmount(converted, homeCurrency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                NeonPill(text = if (isIncome) "In" else "Out", accent = amountTint)
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String, action: String) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            com.expensetracker.app.ui.mascot.Piggy(
                mood = com.expensetracker.app.ui.mascot.PiggyMood.Curious,
                size = 120.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(action, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun formatAmount(amountMinor: Long): String {
    val rupees = amountMinor / 100
    val paise  = amountMinor % 100
    return "\u20B9$rupees${if (paise > 0) ".${paise.toString().padStart(2, '0')}" else ""}"
}

private fun formatAverageAmount(amountMinor: Long, divisor: Int): String {
    if (divisor <= 0) return formatAmount(amountMinor)
    val amount = amountMinor.toDouble() / divisor.toDouble() / 100.0
    return String.format(Locale.ENGLISH, "\u20B9%.0f", amount)
}
