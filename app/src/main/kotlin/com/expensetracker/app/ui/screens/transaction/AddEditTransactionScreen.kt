@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.transaction

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.model.Account
import com.expensetracker.app.core.model.Tag
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.ui.theme.DetailTopBar
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
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
    var showNotes by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        transactionId?.let { viewModel.loadTransaction(it) }
    }
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }
    LaunchedEffect(uiState.notes) {
        if (uiState.notes.isNotEmpty()) showNotes = true
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title          = if (transactionId == null) "New Entry" else "Edit Entry",
                eyebrow        = if (transactionId == null) "Add" else "Edit",
                onNavigateBack = onNavigateBack
            ) {
                if (transactionId != null) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(horizontal = ScreenEdgePadding)
                    .padding(bottom = 28.dp, top = 8.dp)
            ) {
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Button(
                    onClick = { viewModel.saveTransaction() },
                    enabled = uiState.amount.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .appButtonSizing()
                        .testTag("transaction_save_button")
                ) {
                    Text(
                        text = if (transactionId == null) "Save Transaction" else "Save Changes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenEdgePadding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Transaction type — single segmented row
                TypeSegmentedRow(
                    selectedType = uiState.transactionType,
                    onTypeChange = viewModel::updateTransactionType
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Amount — large and prominent
                AmountInput(
                    amount = uiState.amount,
                    onAmountChange = viewModel::updateAmount,
                    isExpense = uiState.transactionType == TransactionType.EXPENSE
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date + Account side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TransactionDatePicker(
                            selectedDate = uiState.transactionTime.toLocalDate(),
                            onDateChange = viewModel::updateTransactionDate
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AccountSelector(
                            selectedAccount = uiState.accountId,
                            accounts = uiState.accounts,
                            onAccountChange = viewModel::updateAccount
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Uber, Starbucks") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_description_input"),
                    colors = fieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags inline
                TagSelector(
                    selectedTags = uiState.selectedTags,
                    tags = uiState.tags,
                    onTagToggle = viewModel::toggleTag,
                    onCreateTag = viewModel::createTag
                )

                // Notes — hidden until tapped
                if (showNotes) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::updateNotes,
                        label = { Text("Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_notes_input"),
                        maxLines = 2,
                        colors = fieldColors()
                    )
                } else {
                    TextButton(
                        onClick = { showNotes = true },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Add note", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (transactionId != null) {
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
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TypeSegmentedRow(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit
) {
    val types = listOf(
        TransactionType.EXPENSE to "Expense",
        TransactionType.INCOME to "Income",
        TransactionType.TRANSFER to "Transfer",
        TransactionType.REFUND to "Refund"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        types.forEach { (type, label) ->
            val selected = selectedType == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else Color.Transparent
                    )
                    .clickable { onTypeChange(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDatePicker(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yy") }

    OutlinedTextField(
        value = selectedDate.format(formatter),
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Choose date", modifier = Modifier.size(18.dp))
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
                        pickerState.selectedDateMillis?.toLocalDate()?.let(onDateChange)
                        showDatePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
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
        leadingIcon = { Text("₹", style = MaterialTheme.typography.titleLarge) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.headlineMedium.copy(
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
            value = accounts.find { it.id == selectedAccount }?.name ?: "Account",
            onValueChange = {},
            readOnly = true,
            label = { Text("Account") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true,
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tags) { tag ->
                FilterChip(
                    selected = selectedTags.contains(tag.id),
                    onClick = { onTagToggle(tag.id) },
                    label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(
                            android.graphics.Color.parseColor(tag.colorHex)
                        ).copy(alpha = 0.3f)
                    )
                )
            }
        }
        IconButton(
            onClick = {
                newTagName = ""
                showCreateDialog = true
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New tag", modifier = Modifier.size(16.dp))
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
