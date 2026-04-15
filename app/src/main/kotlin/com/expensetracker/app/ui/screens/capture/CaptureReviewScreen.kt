@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.expensetracker.app.capture.CaptureSuggestion
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.appButtonSizing
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: CaptureReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var hasSmsPermission by remember { mutableStateOf(hasSmsAccess(context)) }
    var notificationAccessEnabled by remember { mutableStateOf(hasNotificationAccess(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasSmsPermission = hasSmsAccess(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = hasSmsAccess(context)
                notificationAccessEnabled = hasNotificationAccess(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                        Text("Capture Inbox")
                        Text(
                            text = "Review parsed payments before importing",
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
            item {
                CaptureAccessCard(
                    hasSmsPermission = hasSmsPermission,
                    notificationAccessEnabled = notificationAccessEnabled,
                    isImportingSms = uiState.isImportingSms,
                    onGrantSmsAccess = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS
                            )
                        )
                    },
                    onImportRecentSms = viewModel::importRecentSms,
                    onOpenNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }

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
private fun CaptureAccessCard(
    hasSmsPermission: Boolean,
    notificationAccessEnabled: Boolean,
    isImportingSms: Boolean,
    onGrantSmsAccess: () -> Unit,
    onImportRecentSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.primary
    ) {
        SectionHeader(
            eyebrow = "Capture",
            title = if (hasSmsPermission) "SMS import is ready" else "Allow SMS access to get started",
            subtitle = "Import bank, UPI, and shopping messages from SMS, then keep new captures flowing through notifications."
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            StatusTile(
                icon = Icons.Default.MarkEmailRead,
                label = "SMS access",
                value = if (hasSmsPermission) "Ready" else "Required",
                modifier = Modifier.fillMaxWidth(0.48f)
            )
            StatusTile(
                icon = Icons.Default.Notifications,
                label = "Listener",
                value = if (notificationAccessEnabled) "Enabled" else "Optional",
                modifier = Modifier.fillMaxWidth(0.48f)
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            OutlinedButton(
                onClick = onGrantSmsAccess,
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .appButtonSizing()
                    .testTag("capture_request_sms_button")
            ) {
                Text(if (hasSmsPermission) "Refresh Access" else "Grant Access")
            }
            Button(
                onClick = onImportRecentSms,
                enabled = hasSmsPermission && !isImportingSms,
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .appButtonSizing()
                    .testTag("capture_import_sms_button")
            ) {
                Text(if (isImportingSms) "Importing..." else "Import SMS History")
            }
        }

        OutlinedButton(
            onClick = onOpenNotificationSettings,
            modifier = Modifier
                .fillMaxWidth()
                .appButtonSizing()
        ) {
            Text(if (notificationAccessEnabled) "Manage Notification Access" else "Enable Notification Access")
        }
    }
}

@Composable
private fun StatusTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
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

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            Button(
                onClick = onAddToLedger,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .appButtonSizing()
            ) {
                Text(if (isBusy) "Working..." else "Add to Ledger")
            }
            OutlinedButton(
                onClick = onIgnore,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .appButtonSizing()
            ) {
                Text("Ignore")
            }
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
                text = "Import recent SMS or enable notification access to start reviewing inferred transactions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun hasSmsAccess(context: Context): Boolean {
    val readGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED
    val receiveGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED
    return readGranted && receiveGranted
}

private fun hasNotificationAccess(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ).orEmpty()
    return enabledListeners.contains(context.packageName)
}
