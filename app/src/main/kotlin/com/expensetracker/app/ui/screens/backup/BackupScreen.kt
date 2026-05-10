package com.expensetracker.app.ui.screens.backup

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.data.repository.RestoreMode
import com.expensetracker.app.ui.theme.DetailTopBar
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = state.exportJson
        if (uri != null && json != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            }
            viewModel.exportHandled(true)
        } else {
            viewModel.exportHandled(false)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) viewModel.loadImport(json)
        }
    }

    LaunchedEffect(state.exportJson) {
        if (state.exportJson != null) {
            createBackupLauncher.launch("expense-tracker-backup-${System.currentTimeMillis()}.json")
        }
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title          = "Backup & Restore",
                subtitle       = "Export or import all your data",
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(ScreenEdgePadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BackupCard(title = "Export", subtitle = "Creates a local JSON file with your database and preferences. Tagged with your signed-in email.") {
                Button(
                    onClick = viewModel::prepareExport,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.height(1.dp))
                    Text(if (state.busy) "Working..." else "Export backup")
                }
            }

            BackupCard(title = "Import", subtitle = "Select a backup file, then merge or rewrite data. The signed-in email must match the email inside the backup.") {
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Text("Choose backup file")
                }
                state.preview?.let { preview ->
                    Spacer(Modifier.height(12.dp))
                    Text("Backup from ${DateFormat.format("dd MMM yyyy, h:mm a", Date(preview.createdAt))}", fontWeight = FontWeight.Bold)
                    Text("Email: ${preview.backupEmail}", style = MaterialTheme.typography.bodySmall)
                    preview.counts.forEach { (label, count) ->
                        Text("$label: $count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            state.preview?.let {
                RestoreCard(
                    enabled = !state.busy,
                    onMerge = { viewModel.restore(RestoreMode.MERGE) },
                    onRewrite = { viewModel.restore(RestoreMode.REWRITE) }
                )
            }
        }
    }
}

@Composable
private fun BackupCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun RestoreCard(enabled: Boolean, onMerge: () -> Unit, onRewrite: () -> Unit) {
    var confirmRewrite by remember { mutableStateOf(false) }
    BackupCard("Restore options", "Merge keeps existing records. Rewrite deletes current data first.") {
        Button(onClick = onMerge, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Text("Merge backup")
        }
        OutlinedButton(onClick = { confirmRewrite = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Clean rewrite")
        }
    }
    if (confirmRewrite) {
        AlertDialog(
            onDismissRequest = { confirmRewrite = false },
            title = { Text("Replace all current data?") },
            text = { Text("This deletes the current local database and restores only the selected backup.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRewrite = false
                    onRewrite()
                }) { Text("Rewrite") }
            },
            dismissButton = { TextButton(onClick = { confirmRewrite = false }) { Text("Cancel") } }
        )
    }
}
