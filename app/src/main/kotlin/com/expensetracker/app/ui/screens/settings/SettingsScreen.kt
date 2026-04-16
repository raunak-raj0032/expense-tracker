package com.expensetracker.app.ui.screens.settings

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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.theme.AccentDivider
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
    isDarkModeEnabled: Boolean = true,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    var showBackupDialog  by remember { mutableStateOf(false) }
    var showResetDialog   by remember { mutableStateOf(false) }
    var showAboutDialog   by remember { mutableStateOf(false) }
    var visible           by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    LaunchedEffect(Unit) { delay(60); visible = true }

    fun showPrototypeMessage(title: String) {
        scope.launch { snackbarHostState.showSnackbar("$title is not available in this build yet.") }
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

            // Data
            item { SettingsSectionLabel("Data") }
            item { SettingsItem(Icons.Default.AccountBalance, "Accounts",    "Manage your accounts",         accent = MaterialTheme.colorScheme.secondary) { showPrototypeMessage("Accounts") } }
            item { SettingsItem(Icons.Default.Category,       "Categories",  "Manage categories",            accent = MaterialTheme.colorScheme.secondary) { showPrototypeMessage("Categories") } }
            item { SettingsItem(Icons.AutoMirrored.Filled.Label, "Tags",     "Manage tags",                  accent = MaterialTheme.colorScheme.secondary) { showPrototypeMessage("Tags") } }

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
            item { SettingsItem(Icons.AutoMirrored.Filled.Rule, "Rules",       "Automation rules",                  accent = MaterialTheme.colorScheme.tertiary) { showPrototypeMessage("Rules") } }
            item { SettingsItem(Icons.Default.Receipt,           "Suggestions", "Review inferred transactions",      accent = MaterialTheme.colorScheme.tertiary, onClick = onOpenCaptureInbox) }

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
            item { SettingsItem(Icons.Default.Lock, "App Lock", "Lock app with PIN or biometric", accent = MaterialTheme.colorScheme.primary) { showPrototypeMessage("App Lock") } }

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
                                Text("Reset All Data", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                Text("Permanently delete all transactions and settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title            = { Text("Reset All Data?", fontWeight = FontWeight.ExtraBold) },
            text             = { Text("This permanently deletes all transactions, accounts, categories, and settings. This cannot be undone.") },
            confirmButton    = {
                TextButton(
                    onClick = { showResetDialog = false; showPrototypeMessage("Reset All Data") },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
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
