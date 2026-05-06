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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.model.Account
import com.expensetracker.app.core.model.Tag
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.ui.screens.home.TransactionListItem
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.AccentDivider
import com.expensetracker.app.ui.theme.DetailTopBar
import com.expensetracker.app.ui.theme.MainTopBar
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); visible = true }

    val expenseTotal = uiState.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val incomeTotal  = uiState.transactions.filter { it.type == TransactionType.INCOME  }.sumOf { it.amountMinor }
    val netTotal     = incomeTotal - expenseTotal

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (uiState.selectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedTransactionIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Default.Close, "Cancel", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = { showBulkDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else if (onNavigateBack != null) {
                DetailTopBar(
                    title          = "History",
                    subtitle       = "Full transaction record",
                    accent         = MaterialTheme.colorScheme.secondary,
                    onNavigateBack = onNavigateBack
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(badge = {
                                if (uiState.activeFilterCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(uiState.activeFilterCount.toString())
                                    }
                                }
                            }) {
                                Icon(Icons.Default.FilterList, "Filter", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else {
                MainTopBar(
                    title    = "History",
                    subtitle = "Search, filter, and edit",
                    accent   = MaterialTheme.colorScheme.secondary
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(badge = {
                                if (uiState.activeFilterCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(uiState.activeFilterCount.toString())
                                    }
                                }
                            }) {
                                Icon(Icons.Default.FilterList, "Filter", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
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
            text = { Text("This will remove the selected entries from your ledger. You can still recover them by restoring a backup.") },
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

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest   = { showFilterSheet = false },
            containerColor     = MaterialTheme.colorScheme.surface,
            shape              = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            FilterSheet(
                state              = uiState,
                onTypesChange      = viewModel::updateSelectedTypes,
                onAccountsChange   = viewModel::updateSelectedAccounts,
                onTagsChange       = viewModel::updateSelectedTags,
                onDatePreset       = viewModel::setDatePreset,
                onCustomRange      = viewModel::setCustomDateRange,
                onMinAmountChange  = viewModel::updateMinAmount,
                onMaxAmountChange  = viewModel::updateMaxAmount,
                onApply            = { viewModel.applyFilters(); showFilterSheet = false },
                onReset            = { viewModel.resetFilters(); showFilterSheet = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    state: LedgerUiState,
    onTypesChange: (Set<TransactionType>) -> Unit,
    onAccountsChange: (Set<Long>) -> Unit,
    onTagsChange: (Set<Long>) -> Unit,
    onDatePreset: (DateRangePreset) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onMinAmountChange: (String) -> Unit,
    onMaxAmountChange: (String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .padding(horizontal = ScreenEdgePadding, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("FILTER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp)
            Text("Refine results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("Combine any filters below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        FilterSection(title = "Type") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income",
                    TransactionType.TRANSFER to "Transfer",
                    TransactionType.REFUND to "Refund"
                ).forEach { (type, label) ->
                    val selected = state.selectedTypes.contains(type)
                    val accent = when (type) {
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                        TransactionType.INCOME -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onTypesChange(if (selected) state.selectedTypes - type else state.selectedTypes + type)
                        },
                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.15f),
                            selectedLabelColor = accent
                        ),
                        modifier = Modifier.testTag("ledger_filter_${label.lowercase()}")
                    )
                }
            }
        }

        FilterSection(title = "Date") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    DateRangePreset.ALL to "All time",
                    DateRangePreset.THIS_MONTH to "This month",
                    DateRangePreset.LAST_MONTH to "Last month",
                    DateRangePreset.LAST_90_DAYS to "Last 90 days",
                    DateRangePreset.YEAR_TO_DATE to "Year to date"
                ).forEach { (preset, label) ->
                    FilterChip(
                        selected = state.datePreset == preset,
                        onClick = { onDatePreset(preset) },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = state.datePreset == DateRangePreset.CUSTOM,
                    onClick = { showCustomPicker = true },
                    label = {
                        val text = if (state.datePreset == DateRangePreset.CUSTOM && state.startDate != null && state.endDate != null) {
                            val f = DateTimeFormatter.ofPattern("MMM d")
                            "${state.startDate.format(f)} – ${state.endDate.format(f)}"
                        } else "Custom"
                        Text(text)
                    },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        FilterSection(title = "Amount range") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.minAmount,
                    onValueChange = onMinAmountChange,
                    label = { Text("Min") },
                    leadingIcon = { Text("\u20B9") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("ledger_filter_min"),
                    colors = filterFieldColors()
                )
                OutlinedTextField(
                    value = state.maxAmount,
                    onValueChange = onMaxAmountChange,
                    label = { Text("Max") },
                    leadingIcon = { Text("\u20B9") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("ledger_filter_max"),
                    colors = filterFieldColors()
                )
            }
        }

        if (state.accounts.isNotEmpty()) {
            FilterSection(title = "Accounts") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.accounts.forEach { account ->
                        val selected = state.selectedAccountIds.contains(account.id)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onAccountsChange(if (selected) state.selectedAccountIds - account.id else state.selectedAccountIds + account.id)
                            },
                            label = { Text(account.name) }
                        )
                    }
                }
            }
        }

        if (state.tags.isNotEmpty()) {
            FilterSection(title = "Tags") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.tags.forEach { tag ->
                        val selected = state.selectedTagIds.contains(tag.id)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onTagsChange(if (selected) state.selectedTagIds - tag.id else state.selectedTagIds + tag.id)
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val layout = adaptiveFlowLayout(maxWidth, 156.dp, 8.dp, 2)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = layout.columns
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(layout.itemFraction).appButtonSizing().testTag("ledger_filter_reset"),
                    shape = MaterialTheme.shapes.large
                ) { Text("Reset") }
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(layout.itemFraction).appButtonSizing().testTag("ledger_filter_apply"),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("Apply filters") }
            }
        }
    }

    if (showCustomPicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.startDate?.toEpochMillisStart(),
            initialSelectedEndDateMillis = state.endDate?.toEpochMillisStart(),
            initialDisplayMode = androidx.compose.material3.DisplayMode.Input
        )
        DatePickerDialog(
            onDismissRequest = { showCustomPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val startMs = pickerState.selectedStartDateMillis
                    val endMs = pickerState.selectedEndDateMillis
                    if (startMs != null && endMs != null) {
                        onCustomRange(startMs.toLocalDate(), endMs.toLocalDate())
                    }
                    showCustomPicker = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = pickerState,
                title = { Text("Custom range", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
            )
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.0.sp
        )
        content()
    }
}

@Composable
private fun filterFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
)

private fun LocalDate.toEpochMillisStart(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
