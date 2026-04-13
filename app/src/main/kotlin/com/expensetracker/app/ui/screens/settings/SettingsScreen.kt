package com.expensetracker.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenCaptureInbox: () -> Unit = {},
    isDarkModeEnabled: Boolean = true,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    var showBackupDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showPrototypeMessage(title: String) {
        scope.launch {
            snackbarHostState.showSnackbar("$title is not available in this prototype yet.")
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings")
                        Text(
                            text = "Manage preferences, capture, and exports",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("settings_list"),
            contentPadding = PaddingValues(
                start = ScreenEdgePadding,
                top = 8.dp,
                end = ScreenEdgePadding,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.primary
                ) {
                    SectionHeader(
                        eyebrow = "Preferences",
                        title = "Everything in one place",
                        subtitle = "Adjust appearance, review capture tools, and manage the prototype from a cleaner settings hub."
                    )
                }
            }

            item { SettingsSection(title = "Appearance") }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Enabled by default for a calmer low-light experience",
                    checked = isDarkModeEnabled,
                    onCheckedChange = onDarkModeChange
                )
            }

            item { SettingsSection(title = "Data") }
            item {
                SettingsItem(
                    icon = Icons.Default.AccountBalance,
                    title = "Accounts",
                    subtitle = "Manage your accounts",
                    onClick = { showPrototypeMessage("Accounts") }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = "Categories",
                    subtitle = "Manage categories",
                    onClick = { showPrototypeMessage("Categories") }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Label,
                    title = "Tags",
                    subtitle = "Manage tags",
                    onClick = { showPrototypeMessage("Tags") }
                )
            }

            item { SettingsSection(title = "Automation") }
            item {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "SMS & Notification Capture",
                    subtitle = "Parse UPI, bank, and shopping payment messages",
                    modifier = Modifier.testTag("settings_capture_inbox"),
                    onClick = onOpenCaptureInbox
                )
            }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Rule,
                    title = "Rules",
                    subtitle = "Automation rules",
                    onClick = { showPrototypeMessage("Rules") }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Receipt,
                    title = "Suggestions",
                    subtitle = "Review inferred transactions before importing",
                    onClick = onOpenCaptureInbox
                )
            }

            item { SettingsSection(title = "Budget") }
            item {
                SettingsItem(
                    icon = Icons.Default.Savings,
                    title = "Budget",
                    subtitle = "Set monthly budgets",
                    onClick = { showPrototypeMessage("Budget") }
                )
            }

            item { SettingsSection(title = "Data Management") }
            item {
                SettingsItem(
                    icon = Icons.Default.Upload,
                    title = "Export",
                    subtitle = "Export to CSV",
                    onClick = { showPrototypeMessage("Export") }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Backup",
                    subtitle = "Create or restore backup",
                    modifier = Modifier.testTag("settings_backup"),
                    onClick = { showBackupDialog = true }
                )
            }

            item { SettingsSection(title = "Security") }
            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "App Lock",
                    subtitle = "Lock app with PIN or biometric",
                    onClick = { showPrototypeMessage("App Lock") }
                )
            }

            item { SettingsSection(title = "About") }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version 1.0.0",
                    modifier = Modifier.testTag("settings_about"),
                    onClick = { showAboutDialog = true }
                )
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_reset_all"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showResetDialog = true }
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Column(modifier = Modifier.weight(6f)) {
                            Text(
                                text = "Reset All Data",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Delete all transactions and settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will permanently delete all your transactions, accounts, categories, and settings. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        showPrototypeMessage("Reset All Data")
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Restore") },
            text = { Text("Backup and restore flows are not wired in this prototype yet, but the entry point is in place.") },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Expense Tracker") },
            text = {
                Text(
                    "Version 1.0.0\n\nOffline-first expense tracking prototype with manual entry, calendar, analytics, notification capture, SMS parsing, and reviewable transaction suggestions."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
