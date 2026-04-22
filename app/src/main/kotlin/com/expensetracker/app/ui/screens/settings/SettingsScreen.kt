package com.expensetracker.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ai.AiAvailability
import com.expensetracker.app.ai.AiCompatibility
import com.expensetracker.app.core.money.CurrencyConverter
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.NeonPill
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.CardSpacing
import com.expensetracker.app.ui.theme.SectionSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenCaptureInbox: () -> Unit = {},
    onOpenBudget: () -> Unit = {},
    onOpenStatementImport: () -> Unit = {},
    onOpenTags: () -> Unit = {},
    isDarkModeEnabled: Boolean = true,
    onDarkModeChange: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showBackupDialog  by remember { mutableStateOf(false) }
    var showResetDialog   by remember { mutableStateOf(false) }
    var showAboutDialog   by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var visible           by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val homeCurrency by viewModel.homeCurrency.collectAsStateWithLifecycle()
    val resettingData by viewModel.resettingData.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val aiAvailability by viewModel.aiAvailability.collectAsStateWithLifecycle()
    val aiEnabled by viewModel.aiEnabled.collectAsStateWithLifecycle()
    val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBatteryOptSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { delay(60); visible = true }

    fun showPrototypeMessage(title: String) {
        scope.launch { snackbarHostState.showSnackbar("$title is not available in this build yet.") }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost   = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                            Text("Pocket HQ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("Controls, exports & account setup", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("settings_list"),
            contentPadding = PaddingValues(start = ScreenEdgePadding, top = CardSpacing, end = ScreenEdgePadding, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            // Hero control-center card
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing), initialOffsetY = { -20 })) {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
                        SectionHeader(
                            eyebrow  = "Control Center",
                            title    = "Keep your setup tidy",
                            subtitle = "Appearance, automation, exports, and account tools."
                        )
                    }
                }
            }

            // Appearance
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 60))) {
                    SettingsSectionLabel("Appearance")
                }
            }
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 80))) {
                    SettingsToggleItem(
                        icon     = Icons.Default.DarkMode,
                        title    = "Dark Mode",
                        subtitle = "Enabled by default for a calmer low-light experience",
                        checked  = isDarkModeEnabled,
                        accent   = MaterialTheme.colorScheme.primary,
                        onCheckedChange = onDarkModeChange
                    )
                }
            }

            item {
                SettingsItem(
                    icon = Icons.Default.CurrencyExchange,
                    title = "Home currency",
                    subtitle = "Totals convert to $homeCurrency",
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { showCurrencyDialog = true }
                )
            }

            // Data
            item { SettingsSectionLabel("Data") }
            item { SettingsItem(Icons.Default.AccountBalance, "Accounts",    "Manage your accounts",         accent = MaterialTheme.colorScheme.secondary) { showPrototypeMessage("Accounts") } }
            item { SettingsItem(Icons.Default.Category,       "Categories",  "Manage categories",            accent = MaterialTheme.colorScheme.secondary) { showPrototypeMessage("Categories") } }
            item { SettingsItem(Icons.AutoMirrored.Filled.Label, "Tags",     "Manage tags",                  accent = MaterialTheme.colorScheme.secondary, onClick = onOpenTags) }

            // Automation
            item { SettingsSectionLabel("Automation") }
            item {
                SettingsItem(
                    icon     = Icons.Default.Notifications,
                    title    = "SMS & Notification Capture",
                    subtitle = "Parse UPI, bank, and payment messages",
                    accent   = MaterialTheme.colorScheme.tertiary,
                    badge    = "Live",
                    modifier = Modifier.testTag("settings_capture_inbox"),
                    onClick  = onOpenCaptureInbox
                )
            }
            item {
                val accessibilityOn = hasAccessibilityAccess(context)
                SettingsItem(
                    icon     = Icons.Default.PhoneAndroid,
                    title    = "UPI app capture",
                    subtitle = "Detect payments on PhonePe, GPay, Paytm & more as you make them. You confirm each one.",
                    accent   = MaterialTheme.colorScheme.tertiary,
                    badge    = if (accessibilityOn) "On" else "Off",
                    onClick  = {
                        if (!accessibilityOn) {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } else if (!isBatteryOptimizationIgnored(context)) {
                            showBatteryOptSheet = true
                        } else {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }
            item { SettingsItem(Icons.AutoMirrored.Filled.Rule, "Rules",       "Automation rules",                  accent = MaterialTheme.colorScheme.tertiary) { showPrototypeMessage("Rules") } }
            item { SettingsItem(Icons.Default.Receipt,           "Suggestions", "Review inferred transactions",      accent = MaterialTheme.colorScheme.tertiary, onClick = onOpenCaptureInbox) }

            // On-device AI
            item { SettingsSectionLabel("On-device AI") }
            item {
                AiSettingsCard(
                    availability = aiAvailability,
                    aiEnabled = aiEnabled,
                    busy = aiBusy,
                    onToggleEnabled = viewModel::setAiEnabled,
                    onDownload = viewModel::downloadAiModel,
                    onDelete = viewModel::deleteAiModel
                )
            }

            // Budget
            item { SettingsSectionLabel("Budget") }
            item { SettingsItem(Icons.Default.Savings, "Budget", "Set monthly budgets", accent = MaterialTheme.colorScheme.primary, onClick = onOpenBudget) }

            // Data Management
            item { SettingsSectionLabel("Data Management") }
            item { SettingsItem(Icons.Default.Upload, "Statements & CSV Import", "Parse bank or card statements", accent = MaterialTheme.colorScheme.secondary, onClick = onOpenStatementImport) }
            item {
                SettingsItem(
                    icon     = Icons.Default.Download,
                    title    = "Backup",
                    subtitle = "Create or restore backup",
                    accent   = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("settings_backup"),
                    onClick  = { showBackupDialog = true }
                )
            }

            // Security
            item { SettingsSectionLabel("Security") }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Lock,
                    title = "App Lock",
                    subtitle = "Require face or fingerprint before the app opens",
                    checked = biometricEnabled,
                    accent = MaterialTheme.colorScheme.primary,
                    onCheckedChange = viewModel::setBiometricEnabled
                )
            }

            // About
            item { SettingsSectionLabel("About") }
            item {
                SettingsItem(
                    icon     = Icons.Default.Info,
                    title    = "About",
                    subtitle = "Version 1.0.0",
                    accent   = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("settings_about"),
                    onClick  = { showAboutDialog = true }
                )
            }

            // Danger zone
            item {
                AnimatedVisibility(visible, enter = fadeIn(tween(500, 200))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reset_all")
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                            )
                            .clickable { showResetDialog = true }
                            .padding(18.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete All Entries", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                Text("Permanently delete all local transactions, imports, budgets, and suggestions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title            = { Text("Delete All Entries?", fontWeight = FontWeight.ExtraBold) },
            text             = { Text("This permanently deletes all local transactions, imported statement results, capture suggestions, budgets, and other ledger entries. Default starter accounts, categories, and tags will be recreated.") },
            confirmButton    = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.deleteAllEntries()
                    },
                    enabled = !resettingData,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(if (resettingData) "Deleting..." else "Delete everything") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !resettingData
                ) { Text("Cancel") }
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title            = { Text("Backup & Restore", fontWeight = FontWeight.ExtraBold) },
            text             = { Text("Backup and restore are not wired in this prototype yet, but the entry point is in place.") },
            confirmButton    = { TextButton(onClick = { showBackupDialog = false }) { Text("Got it") } }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Home currency", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CurrencyConverter.supported.forEach { code ->
                        val selected = code == homeCurrency
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    viewModel.setHomeCurrency(code)
                                    showCurrencyDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${CurrencyConverter.symbol(code)}  $code",
                                modifier = Modifier.weight(1f),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (selected) Text("Selected", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") } }
        )
    }

    if (showBatteryOptSheet) {
        AlertDialog(
            onDismissRequest = { showBatteryOptSheet = false },
            title = { Text("Keep UPI capture alive", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Android aggressively kills background services to save battery. Exempt Pocket Pulse so UPI app capture keeps working when the app is closed.")
                    Text("You'll be taken to the system prompt where you can allow background activity.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryOptSheet = false
                    runCatching {
                        @Suppress("BatteryLife")
                        context.startActivity(
                            Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onFailure {
                        context.startActivity(
                            Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("Allow background activity") }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryOptSheet = false }) { Text("Not now") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title            = { Text("About Pocket Pulse", fontWeight = FontWeight.ExtraBold) },
            text             = { Text("Version 1.0.0\n\nOffline-first expense tracking with manual entry, calendar, analytics, notification capture, SMS parsing, and reviewable transaction suggestions.") },
            confirmButton    = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun SettingsSectionLabel(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        letterSpacing = 1.2.sp
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color = MaterialTheme.colorScheme.primary,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked         = checked,
                onCheckedChange = onCheckedChange,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor      = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor      = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor    = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor    = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    badge?.let { NeonPill(text = it, accent = accent) }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

// Keep old function name for compatibility
@Composable
fun SettingsSection(title: String) = SettingsSectionLabel(title)

@Composable
private fun AiSettingsCard(
    availability: AiAvailability,
    aiEnabled: Boolean,
    busy: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)) }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Ledger AI Search", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(aiStatusLine(availability), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (availability !is AiAvailability.Unsupported) {
                    Switch(
                        checked = aiEnabled,
                        onCheckedChange = onToggleEnabled,
                        enabled = !busy && availability is AiAvailability.Ready,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            when (val state = availability) {
                is AiAvailability.Unsupported -> {
                    Text(
                        unsupportedMessage(state.reasons),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AiAvailability.NeedsDownload -> {
                    TextButton(
                        onClick = onDownload,
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = accent)
                    ) { Text(if (busy) "Preparing..." else "Download model") }
                }
                is AiAvailability.Downloading -> {
                    val pct = if (state.totalBytes > 0) (state.downloadedBytes * 100 / state.totalBytes) else 0
                    Text(
                        "Downloading... $pct% (${formatBytes(state.downloadedBytes)} / ${formatBytes(state.totalBytes)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AiAvailability.Ready -> {
                    TextButton(
                        onClick = onDelete,
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(if (busy) "Working..." else "Remove model") }
                }
                AiAvailability.Initializing -> {
                    Text("Checking device...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is AiAvailability.Error -> {
                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(
                        onClick = onDownload,
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = accent)
                    ) { Text("Retry") }
                }
            }
        }
    }
}

private fun aiStatusLine(availability: AiAvailability): String = when (availability) {
    is AiAvailability.Unsupported -> "Not available on this device"
    AiAvailability.NeedsDownload -> "Download required. Runs fully offline, nothing leaves your device."
    is AiAvailability.Downloading -> "Downloading model..."
    AiAvailability.Ready -> "Ready. Ask questions about your ledger in plain English."
    AiAvailability.Initializing -> "Checking compatibility..."
    is AiAvailability.Error -> "Something went wrong"
}

private fun unsupportedMessage(reasons: List<AiCompatibility.Reason>): String {
    if (reasons.isEmpty()) return "Device not supported."
    val phrases = reasons.map {
        when (it) {
            AiCompatibility.Reason.UNSUPPORTED_ABI -> "needs a 64-bit ARM device"
            AiCompatibility.Reason.API_TOO_OLD -> "needs Android 12 or newer"
            AiCompatibility.Reason.EMULATOR -> "emulators are not supported"
            AiCompatibility.Reason.LOW_RAM_DEVICE -> "device is flagged as low-RAM"
            AiCompatibility.Reason.INSUFFICIENT_RAM -> "needs at least 8 GB RAM"
            AiCompatibility.Reason.INSUFFICIENT_STORAGE -> "needs at least 2 GB free storage"
        }
    }
    return "On-device AI ${phrases.joinToString("; ")}."
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit += 1
    }
    return "%.1f %s".format(value, units[unit])
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun hasAccessibilityAccess(context: Context): Boolean {
    val enabled = AndroidSettings.Secure.getString(
        context.contentResolver,
        AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    val expected = "${context.packageName}/com.expensetracker.app.capture.UpiAccessibilityService"
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
