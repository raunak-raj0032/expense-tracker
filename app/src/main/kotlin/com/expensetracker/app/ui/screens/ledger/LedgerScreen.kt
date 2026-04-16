@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.ledger

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.ui.screens.home.TransactionListItem
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.CardSpacing
import com.expensetracker.app.ui.theme.SectionSpacing
import com.expensetracker.app.ui.theme.adaptiveFlowLayout
import com.expensetracker.app.ui.theme.appButtonSizing
import com.expensetracker.app.ui.theme.financialFigures
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    onTransactionClick: (Long) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: LedgerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val expenseTotal = uiState.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amountMinor }
    val incomeTotal = uiState.transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amountMinor }
    val netTotal = incomeTotal - expenseTotal

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("History")
                        Text(
                            text = if (onNavigateBack != null) {
                                "A full record of every check-in"
                            } else {
                                "Search, filter, and edit your money timeline"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    onNavigateBack?.let { navigateBack ->
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = ScreenEdgePadding,
                top = CardSpacing,
                end = ScreenEdgePadding,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.primary
                ) {
                    SectionHeader(
                        eyebrow = "Timeline",
                        title = "${uiState.transactions.size} transactions",
                        subtitle = if (uiState.searchQuery.isBlank()) {
                            "Your complete running history in one friendly feed."
                        } else {
                            "Showing results for \"${uiState.searchQuery}\""
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LedgerOverviewCard(
                            label = "Expense",
                            value = formatAmount(expenseTotal),
                            accent = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        LedgerOverviewCard(
                            label = "Income",
                            value = formatAmount(incomeTotal),
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        LedgerOverviewCard(
                            label = if (uiState.searchQuery.isBlank()) "Net" else "Filtered",
                            value = "${if (netTotal >= 0) "+" else "-"}${formatAmount(kotlin.math.abs(netTotal))}",
                            accent = if (netTotal >= 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.transactions.isEmpty()) {
                item {
                    EmptyLedgerState()
                }
            } else {
                val groupedTransactions = uiState.transactions.groupBy {
                    it.transactionTime.toLocalDate()
                }

                groupedTransactions.forEach { (date, transactions) ->
                    val dayNet = transactions.sumOf { transaction ->
                        if (transaction.type == TransactionType.INCOME) {
                            transaction.amountMinor
                        } else {
                            -transaction.amountMinor
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                            ) {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            Text(
                                text = "${if (dayNet >= 0) "+" else "-"}${formatAmount(kotlin.math.abs(dayNet))}",
                                style = MaterialTheme.typography.labelLarge.financialFigures(FontWeight.SemiBold),
                                color = if (dayNet >= 0) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    items(transactions) { transaction ->
                        TransactionListItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FilterSheet(
                selectedTypes = uiState.selectedTypes,
                selectedAccounts = uiState.selectedAccountIds,
                onTypesChange = viewModel::updateSelectedTypes,
                onAccountsChange = viewModel::updateSelectedAccounts,
                onApply = {
                    viewModel.applyFilters()
                    showFilterSheet = false
                },
                onReset = {
                    viewModel.resetFilters()
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
private fun LedgerOverviewCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.financialFigures(FontWeight.ExtraBold),
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier,
        accent = MaterialTheme.colorScheme.primary,
        contentPadding = PaddingValues(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ledger_search_input"),
            placeholder = { Text("Search transactions...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun EmptyLedgerState() {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No transactions found",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Try a different search, reset your filters, or add a new entry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenEdgePadding, vertical = 8.dp),
        accent = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = "Filter Transactions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose which transaction types you want to see.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTypes.contains(TransactionType.EXPENSE),
                onClick = {
                    val newTypes = if (selectedTypes.contains(TransactionType.EXPENSE)) {
                        selectedTypes - TransactionType.EXPENSE
                    } else {
                        selectedTypes + TransactionType.EXPENSE
                    }
                    onTypesChange(newTypes)
                },
                label = { Text("Expense") },
                modifier = Modifier.testTag("ledger_filter_expense")
            )
            FilterChip(
                selected = selectedTypes.contains(TransactionType.INCOME),
                onClick = {
                    val newTypes = if (selectedTypes.contains(TransactionType.INCOME)) {
                        selectedTypes - TransactionType.INCOME
                    } else {
                        selectedTypes + TransactionType.INCOME
                    }
                    onTypesChange(newTypes)
                },
                label = { Text("Income") },
                modifier = Modifier.testTag("ledger_filter_income")
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val actionLayout = adaptiveFlowLayout(
                maxWidth = maxWidth,
                minItemWidth = 156.dp,
                spacing = 8.dp,
                maxColumns = 2
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = actionLayout.columns
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth(actionLayout.itemFraction)
                        .appButtonSizing()
                        .testTag("ledger_filter_reset")
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth(actionLayout.itemFraction)
                        .appButtonSizing()
                        .testTag("ledger_filter_apply")
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
