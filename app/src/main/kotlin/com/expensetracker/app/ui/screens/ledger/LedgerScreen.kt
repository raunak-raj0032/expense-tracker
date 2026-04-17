@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.ledger

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.ui.screens.home.TransactionListItem
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.AccentDivider
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.NeonPill
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.CardSpacing
import com.expensetracker.app.ui.theme.SectionSpacing
import com.expensetracker.app.ui.theme.adaptiveFlowLayout
import com.expensetracker.app.ui.theme.appButtonSizing
import com.expensetracker.app.ui.theme.financialFigures
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    onTransactionClick: (Long) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: LedgerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); visible = true }

    val expenseTotal = uiState.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val incomeTotal  = uiState.transactions.filter { it.type == TransactionType.INCOME  }.sumOf { it.amountMinor }
    val netTotal     = incomeTotal - expenseTotal

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                            Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            text  = if (onNavigateBack != null) "Full transaction record" else "Search, filter, and edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    onNavigateBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                colors  = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, "Filter", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = ScreenEdgePadding, top = CardSpacing, end = ScreenEdgePadding, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            // Overview header card
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing), initialOffsetY = { -20 })) {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) {
                        SectionHeader(
                            eyebrow  = "Timeline",
                            title    = "${uiState.transactions.size} transactions",
                            subtitle = if (uiState.searchQuery.isBlank()) "Your complete running history"
                                       else "Results for \"${uiState.searchQuery}\""
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LedgerOverviewCard("Expense", formatAmount(expenseTotal), MaterialTheme.colorScheme.error,  Modifier.weight(1f))
                            LedgerOverviewCard("Income",  formatAmount(incomeTotal),  MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                            LedgerOverviewCard(
                                label  = if (uiState.searchQuery.isBlank()) "Net" else "Filtered",
                                value  = "${if (netTotal >= 0) "+" else "-"}${formatAmount(kotlin.math.abs(netTotal))}",
                                accent = if (netTotal >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Search bar
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 80))) {
                    SearchBar(query = uiState.searchQuery, onQueryChange = viewModel::updateSearchQuery, modifier = Modifier.fillMaxWidth())
                }
            }

            if (uiState.transactions.isEmpty()) {
                item { EmptyLedgerState() }
            } else {
                val grouped = uiState.transactions.groupBy { it.transactionTime.toLocalDate() }
                grouped.forEach { (date, transactions) ->
                    val dayNet = transactions.sumOf { t ->
                        if (t.type == TransactionType.INCOME) t.amountMinor else -t.amountMinor
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                Text(
                                    text  = date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text  = "${if (dayNet >= 0) "+" else "-"}${formatAmount(kotlin.math.abs(dayNet))}",
                                style = MaterialTheme.typography.labelLarge.financialFigures(FontWeight.SemiBold),
                                color = if (dayNet >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    items(transactions) { transaction ->
                        TransactionListItem(transaction = transaction, onClick = { onTransactionClick(transaction.id) })
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest   = { showFilterSheet = false },
            containerColor     = MaterialTheme.colorScheme.surface,
            shape              = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            FilterSheet(
                selectedTypes    = uiState.selectedTypes,
                selectedAccounts = uiState.selectedAccountIds,
                onTypesChange    = viewModel::updateSelectedTypes,
                onAccountsChange = viewModel::updateSelectedAccounts,
                onApply          = { viewModel.applyFilters(); showFilterSheet = false },
                onReset          = { viewModel.resetFilters(); showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun LedgerOverviewCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent, letterSpacing = 0.5.sp)
            Text(value, style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value         = query,
        onValueChange = onQueryChange,
        modifier      = modifier
            .testTag("ledger_search_input")
            .clip(MaterialTheme.shapes.large),
        placeholder   = { Text("Search transactions...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
        trailingIcon  = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape      = MaterialTheme.shapes.large,
        colors     = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor  = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedBorderColor    = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            focusedContainerColor  = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    )
}

@Composable
fun EmptyLedgerState() {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No transactions found", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Try a different search or reset filters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FilterSheet(
    selectedTypes: Set<TransactionType>,
    selectedAccounts: Set<Long>,
    onTypesChange: (Set<TransactionType>) -> Unit,
    onAccountsChange: (Set<Long>) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenEdgePadding, vertical = 8.dp),
        accent   = MaterialTheme.colorScheme.primary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("FILTER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp)
            Text("Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("Choose which types to display.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        AccentDivider()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (type, label) ->
                val selected = selectedTypes.contains(type)
                val accent   = if (type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                FilterChip(
                    selected = selected,
                    onClick  = {
                        onTypesChange(if (selected) selectedTypes - type else selectedTypes + type)
                    },
                    label    = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.15f),
                        selectedLabelColor     = accent
                    ),
                    modifier = Modifier.testTag("ledger_filter_${label.lowercase()}")
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val layout = adaptiveFlowLayout(maxWidth, 156.dp, 8.dp, 2)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow     = layout.columns
            ) {
                OutlinedButton(
                    onClick  = onReset,
                    modifier = Modifier.fillMaxWidth(layout.itemFraction).appButtonSizing().testTag("ledger_filter_reset"),
                    shape    = MaterialTheme.shapes.large
                ) { Text("Reset") }
                Button(
                    onClick  = onApply,
                    modifier = Modifier.fillMaxWidth(layout.itemFraction).appButtonSizing().testTag("ledger_filter_apply"),
                    shape    = MaterialTheme.shapes.large,
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("Apply filters") }
            }
        }
    }
}
