@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.capture

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.capture.CaptureSuggestion
import com.expensetracker.app.core.model.CaptureSourceType
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.DetailTopBar
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.adaptiveFlowLayout
import com.expensetracker.app.ui.theme.appButtonSizing
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: CaptureReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
            DetailTopBar(
                title          = "Capture Inbox",
                subtitle       = "Captured from SMS, notifications & UPI",
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("capture_inbox_list"),
            contentPadding = PaddingValues(
                start = ScreenEdgePadding,
                top = 8.dp,
                end = ScreenEdgePadding,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.suggestions.isEmpty()) {
                item {
                    EmptyCaptureState()
                }
            } else {
                items(uiState.suggestions, key = { it.id }) { suggestion ->
                    CaptureSuggestionCard(
                        suggestion = suggestion,
                        isBusy = uiState.activeSuggestionId == suggestion.id,
                        onAddToLedger = { viewModel.addSuggestion(suggestion.id) },
                        onIgnore = { viewModel.ignoreSuggestion(suggestion.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureSuggestionCard(
    suggestion: CaptureSuggestion,
    isBusy: Boolean,
    onAddToLedger: () -> Unit,
    onIgnore: () -> Unit
) {
    val directionPrefix = if (suggestion.direction.name == "INCOME") "+" else "-"
    val timestamp = suggestion.receivedAt.format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = if (suggestion.direction.name == "INCOME") {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.tertiary
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Text(
                        text = suggestion.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SourceBadge(sourceType = suggestion.sourceType)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${suggestion.sourceLabel} | $timestamp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$directionPrefix${formatAmount(suggestion.amountMinor)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (suggestion.direction.name == "INCOME") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.ExtraBold
            )
        }

        val detailLine = listOfNotNull(
            suggestion.categoryHint,
            suggestion.paymentMethod,
            suggestion.reference?.let { "Ref $it" },
            if (suggestion.isPeerTransfer) "Peer transfer" else null
        ).joinToString(" | ")

        if (detailLine.isNotBlank()) {
            Text(
                text = detailLine,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (suggestion.rawPreview.isNotBlank()) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            ) {
                Text(
                    text = suggestion.rawPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val actionLayout = adaptiveFlowLayout(
                maxWidth = maxWidth,
                minItemWidth = 152.dp,
                spacing = 8.dp,
                maxColumns = 2
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = actionLayout.columns
            ) {
                Button(
                    onClick = onAddToLedger,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth(actionLayout.itemFraction)
                        .appButtonSizing()
                ) {
                    Text(if (isBusy) "Working..." else "Add to Ledger")
                }
                OutlinedButton(
                    onClick = onIgnore,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth(actionLayout.itemFraction)
                        .appButtonSizing()
                ) {
                    Text("Ignore")
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(sourceType: CaptureSourceType) {
    val (label, icon, accent) = when (sourceType) {
        CaptureSourceType.SMS -> Triple("SMS message", Icons.Default.MarkEmailRead, MaterialTheme.colorScheme.secondary)
        CaptureSourceType.ACCESSIBILITY -> Triple("UPI screen", Icons.Default.PhoneAndroid, MaterialTheme.colorScheme.primary)
        CaptureSourceType.NOTIFICATION -> Triple("Notification", Icons.Default.Notifications, MaterialTheme.colorScheme.tertiary)
        else -> Triple(sourceType.name.lowercase().replaceFirstChar(Char::uppercase), Icons.Default.Stars, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        shape = CircleShape,
        color = accent.copy(alpha = 0.14f),
        contentColor = accent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyCaptureState() {
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
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No captured payments yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Captured SMS, notification, and UPI screen payments will appear here. Manage capture modes from Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
