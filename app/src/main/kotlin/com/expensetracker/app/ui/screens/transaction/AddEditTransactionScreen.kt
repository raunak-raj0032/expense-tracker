package com.expensetracker.app.ui.screens.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.expensetracker.app.ui.theme.adaptiveFlowLayout
import com.expensetracker.app.ui.theme.appButtonSizing
import com.expensetracker.app.ui.theme.financialFigures

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
                top = 8.dp,
                end = ScreenEdgePadding,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.primary
                ) {
                    SectionHeader(
                        eyebrow = if (transactionId == null) "New Entry" else "Edit Entry",
                        title = if (transactionId == null) "Add a transaction" else "Update transaction details",
                        subtitle = "Choose the type, amount, account, and notes so this entry stays easy to review later."
                    )
                    TransactionTypeSelector(
                        selectedType = uiState.transactionType,
                        onTypeChange = viewModel::updateTransactionType
                    )
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (uiState.transactionType == TransactionType.EXPENSE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                ) {
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
                    accent = MaterialTheme.colorScheme.secondary
                ) {
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
                    accent = MaterialTheme.colorScheme.tertiary
                ) {
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::updateNotes,
                        label = { Text("Notes (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_notes_input"),
                        maxLines = 3,
                        colors = fieldColors()
                    )
                    TagSelector(
                        selectedTags = uiState.selectedTags,
                        tags = uiState.tags,
                        onTagToggle = viewModel::toggleTag
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val chipLayout = adaptiveFlowLayout(
            maxWidth = maxWidth,
            minItemWidth = 148.dp,
            spacing = 8.dp,
            maxColumns = 2
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = chipLayout.columns
        ) {
            TransactionType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    leadingIcon = if (selectedType == type) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(chipLayout.itemFraction)
                )
            }
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
        leadingIcon = { Text("\u20B9", style = MaterialTheme.typography.titleLarge) },
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
    onTagToggle: (Long) -> Unit
) {
    Column {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
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
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = Color.Transparent,
    focusedContainerColor = Color.Transparent
)
