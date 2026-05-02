@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.model.Account
import com.expensetracker.app.core.model.Category
import com.expensetracker.app.core.model.Tag
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.appButtonSizing
import com.expensetracker.app.ui.theme.financialFigures
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        transactionId?.let { viewModel.loadTransaction(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (transactionId == null) "Add Transaction" else "Edit Transaction")
                        Text(
                            text = "Track a payment or income",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (transactionId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Transaction")
                        }
                    }
                    TextButton(
                        onClick = { viewModel.saveTransaction() },
                        enabled = uiState.amount.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = ScreenEdgePadding,
                top = 4.dp,
                end = ScreenEdgePadding,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.primary,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    SectionHeader(
                        eyebrow = if (transactionId == null) "New Entry" else "Edit Entry",
                        title = if (transactionId == null) "Transaction details" else "Update details",
                        subtitle = "Type, amount, and account."
                    )
                    TransactionTypeSelector(
                        selectedType = uiState.transactionType,
                        onTypeChange = viewModel::updateTransactionType
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AmountInput(
                        amount = uiState.amount,
                        onAmountChange = viewModel::updateAmount,
                        isExpense = uiState.transactionType == TransactionType.EXPENSE
                    )
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.tertiary,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    TransactionDatePicker(
                        selectedDate = uiState.transactionTime.toLocalDate(),
                        onDateChange = viewModel::updateTransactionDate
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("Description (e.g. Uber, Starbucks)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_description_input"),
                        colors = fieldColors()
                    )
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.secondary,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    CategorySuggestionBanner(
                        suggestedCategoryId = uiState.suggestedCategoryId,
                        categories = uiState.categories,
                        onAccept = viewModel::acceptSuggestedCategory,
                        onDismiss = viewModel::dismissSuggestion
                    )
                    CategorySelector(
                        selectedCategory = uiState.categoryId,
                        categories = uiState.categories,
                        onCategoryChange = viewModel::updateCategory
                    )
                    AccountSelector(
                        selectedAccount = uiState.accountId,
                        accounts = uiState.accounts,
                        onAccountChange = viewModel::updateAccount
                    )
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.tertiary,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::updateNotes,
                        label = { Text("Notes (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_notes_input"),
                        maxLines = 2,
                        colors = fieldColors()
                    )
                    TagSelector(
                        selectedTags = uiState.selectedTags,
                        tags = uiState.tags,
                        onTagToggle = viewModel::toggleTag,
                        onCreateTag = viewModel::createTag
                    )
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.saveTransaction() },
                    enabled = uiState.amount.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appButtonSizing()
                        .testTag("transaction_save_button")
                ) {
                    Text(if (transactionId == null) "Save Transaction" else "Save Changes")
                }
            }

            if (transactionId != null) {
                item {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .appButtonSizing(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Delete Entry")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction?") },
            text = { Text("This will remove the entry from your ledger.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTransaction()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDatePicker(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    OutlinedTextField(
        value = selectedDate.format(formatter),
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Choose date")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true }
            .testTag("transaction_date_input"),
        singleLine = true,
        colors = fieldColors()
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochMillisStart()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis
                            ?.toLocalDate()
                            ?.let(onDateChange)
                        showDatePicker = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                title = {
                    Text(
                        text = "Transaction date",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 2
    ) {
        listOf(
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            TransactionType.TRANSFER,
            TransactionType.REFUND
        ).forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeChange(type) },
                label = {
                    Text(
                        text = transactionTypeLabel(type),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 120.dp)
            )
        }
    }
}

private fun transactionTypeLabel(type: TransactionType): String =
    when (type) {
        TransactionType.EXPENSE -> "Expense"
        TransactionType.INCOME -> "Income"
        TransactionType.TRANSFER -> "Transfer"
        TransactionType.REFUND -> "Refund"
    }

@Composable
fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    isExpense: Boolean
) {
    OutlinedTextField(
        value = amount,
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                val parts = newValue.split(".")
                if (parts.size <= 2 && (parts.size != 2 || parts[1].length <= 2)) {
                    onAmountChange(newValue)
                }
            }
        },
        label = { Text("Amount") },
        leadingIcon = { Text("\u20B9", style = MaterialTheme.typography.titleLarge) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ).financialFigures(FontWeight.ExtraBold),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_amount_input"),
        singleLine = true,
        colors = fieldColors()
    )
}

@Composable
fun CategorySuggestionBanner(
    suggestedCategoryId: Long?,
    categories: List<Category>,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val suggestion = suggestedCategoryId?.let { id -> categories.firstOrNull { it.id == id } } ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Suggested: ${suggestion.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDismiss) { Text("Dismiss") }
        Button(onClick = onAccept) { Text("Apply") }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: Long?,
    categories: List<Category>,
    onCategoryChange: (Long) -> Unit
) {
    Column {
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category.id,
                    onClick = { onCategoryChange(category.id) },
                    label = { Text(category.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelector(
    selectedAccount: Long?,
    accounts: List<Account>,
    onAccountChange: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = accounts.find { it.id == selectedAccount }?.name ?: "Select account",
            onValueChange = {},
            readOnly = true,
            label = { Text("Account") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = fieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onAccountChange(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TagSelector(
    selectedTags: Set<Long>,
    tags: List<Tag>,
    onTagToggle: (Long) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                newTagName = ""
                showCreateDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("New tag")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags) { tag ->
                FilterChip(
                    selected = selectedTags.contains(tag.id),
                    onClick = { onTagToggle(tag.id) },
                    label = { Text(tag.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(
                            android.graphics.Color.parseColor(tag.colorHex)
                        ).copy(alpha = 0.3f)
                    )
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateTag(newTagName)
                        showCreateDialog = false
                    },
                    enabled = newTagName.trim().isNotEmpty()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = Color.Transparent,
    focusedContainerColor = Color.Transparent
)

private fun LocalDate.toEpochMillisStart(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
