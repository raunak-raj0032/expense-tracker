package com.expensetracker.app.ui.screens.imports

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.statement.StatementPreviewEntry
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.StatBadge
import com.expensetracker.app.ui.theme.adaptiveFlowLayout
import com.expensetracker.app.ui.theme.appButtonSizing
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardOptions

private data class PickedDocument(
    val name: String,
    val mimeType: String?,
    val bytes: ByteArray
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatementImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatementImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.readPickedDocument(it)?.let { document ->
                viewModel.previewDocument(
                    documentName = document.name,
                    mimeType = document.mimeType,
                    bytes = document.bytes
                )
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Statement Import")
                        Text(
                            text = "Parse monthly bank or card statements into your ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        eyebrow = "Import",
                        title = "Bring statements into the ledger",
                        subtitle = "Pick a PDF, CSV, or paste statement text, preview the parsed entries, then import only what is not already in your books."
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBadge(
                            label = "Supports",
                            value = "PDF / CSV",
                            modifier = Modifier.weight(1f),
                            accent = MaterialTheme.colorScheme.primary
                        )
                        StatBadge(
                            label = "Paste",
                            value = "Raw text",
                            modifier = Modifier.weight(1f),
                            accent = MaterialTheme.colorScheme.secondary
                        )
                        StatBadge(
                            label = "Duplicates",
                            value = "Skipped",
                            modifier = Modifier.weight(1f),
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        text = "Import Into",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ExposedDropdownMenuBox(
                        expanded = accountDropdownExpanded,
                        onExpandedChange = { accountDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.accounts.firstOrNull { it.id == uiState.selectedAccountId }?.name ?: "Choose account",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            uiState.accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        viewModel.updateSelectedAccount(account.id)
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

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
                            OutlinedButton(
                                onClick = {
                                    documentLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "text/csv",
                                            "text/plain",
                                            "*/*"
                                        )
                                    )
                                },
                                enabled = !uiState.isParsing,
                                modifier = Modifier
                                    .fillMaxWidth(actionLayout.itemFraction)
                                    .appButtonSizing()
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(if (uiState.isParsing) "Reading..." else "Choose File")
                            }
                            Button(
                                onClick = viewModel::previewPastedText,
                                enabled = !uiState.isParsing,
                                modifier = Modifier
                                    .fillMaxWidth(actionLayout.itemFraction)
                                    .appButtonSizing()
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Parse Pasted")
                            }
                        }
                    }

                    uiState.selectedDocumentName?.let { documentName ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Selected file",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = documentName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (uiState.selectedDocumentRequiresPassword) {
                                    Text(
                                        text = "This statement is locked. Enter the PDF password below, then parse the selected file again.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                } else {
                                    Text(
                                        text = "You can re-parse this file without opening the picker again.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.documentPassword,
                        onValueChange = viewModel::updateDocumentPassword,
                        label = { Text("PDF password (optional)") },
                        placeholder = { Text("Enter only for encrypted PDFs") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isParsing,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    }
                                )
                            }
                        }
                    )
                    Text(
                        text = "Used only when opening encrypted PDF statements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.selectedDocumentName != null) {
                        Button(
                            onClick = viewModel::previewSelectedDocument,
                            enabled = !uiState.isParsing && (
                                !uiState.selectedDocumentRequiresPassword ||
                                    uiState.documentPassword.isNotBlank()
                                ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .appButtonSizing()
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                if (uiState.selectedDocumentRequiresPassword) {
                                    "Unlock & Parse Selected File"
                                } else if (uiState.isParsing) {
                                    "Parsing Selected File..."
                                } else {
                                    "Parse Selected File"
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.pastedText,
                        onValueChange = viewModel::updatePastedText,
                        label = { Text("Paste statement text") },
                        placeholder = { Text("Paste bank statement text here...\ne.g.:\n13/04/2026 | Amazon UPI | ₹499.00\n14/04/2026 | Salary Credit | +₹50,000.00") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6
                    )
                    
                    var tipsExpanded by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { tipsExpanded = !tipsExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (tipsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = if (tipsExpanded) "Hide tips" else "Show tips for scanned PDFs",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    
                    if (tipsExpanded) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "If your PDF is scanned (no selectable text), try these alternatives:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "1. Export from bank app/website:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "   • SBI Card: Login → Statements → Download as CSV/Excel\n" +
                                           "   • HDFC/ICICI/SBI: Net Banking → Accounts → Download Statement\n" +
                                           "   • Most banks offer CSV/Excel export in statement section",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "2. Copy-paste from PDF viewer:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "   • Open PDF in browser or Adobe Reader\n" +
                                           "   • Select all text (Ctrl+A) and copy (Ctrl+C)\n" +
                                           "   • Paste into the text box above",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "3. OCR conversion (if available):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "   • Use Google Docs, Adobe Acrobat, or online OCR tools\n" +
                                           "   • Upload scanned PDF → Download as text/CSV → Import",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val preview = uiState.preview
            if (preview == null) {
                item {
                    EmptyStatementState()
                }
            } else {
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = MaterialTheme.colorScheme.tertiary
                    ) {
                        SectionHeader(
                            eyebrow = "Preview",
                            title = preview.sourceName,
                            subtitle = "${preview.entries.size} parsed entries, ${preview.duplicateCount} duplicates, ${preview.ignoredLineCount} ignored lines"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBadge(
                                label = "Expense",
                                value = formatAmount(preview.expenseTotal),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.error
                            )
                            StatBadge(
                                label = "Income",
                                value = formatAmount(preview.incomeTotal),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.secondary
                            )
                            StatBadge(
                                label = "Importable",
                                value = (preview.entries.size - preview.duplicateCount).coerceAtLeast(0).toString(),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = viewModel::importPreview,
                            enabled = !uiState.isImporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .appButtonSizing()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(if (uiState.isImporting) "Importing..." else "Import to Ledger")
                        }
                    }
                }

                itemsIndexed(
                    items = preview.entries.take(24),
                    key = { index, entry -> "${entry.fingerprint}:$index" }
                ) { _, entry ->
                    StatementPreviewCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun StatementPreviewCard(entry: StatementPreviewEntry) {
    val accent = when (entry.direction) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.transactionTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val detail = listOfNotNull(
                    entry.merchantName,
                    entry.paymentMethod,
                    entry.categoryHint,
                    entry.reference?.let { "Ref $it" }
                ).joinToString(" | ")
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(entry.amountMinor),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.isDuplicate) {
                    Text(
                        text = "Duplicate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStatementState() {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No statement preview yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose a monthly PDF, CSV, or paste statement text to preview how it will land in the ledger.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Context.readPickedDocument(uri: Uri): PickedDocument? {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val mimeType = contentResolver.getType(uri)
    val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && columnIndex >= 0) {
                cursor.getString(columnIndex)
            } else {
                null
            }
        }
        ?: uri.lastPathSegment
        ?: "statement"

    return PickedDocument(
        name = name,
        mimeType = mimeType,
        bytes = bytes
    )
}
