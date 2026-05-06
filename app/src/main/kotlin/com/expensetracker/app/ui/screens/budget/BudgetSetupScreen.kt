package com.expensetracker.app.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.DetailTopBar
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.appButtonSizing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title          = "Budget",
                subtitle       = "Set a monthly spending target",
                onNavigateBack = onNavigateBack
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
                        eyebrow = "Monthly Plan",
                        title = "Keep spending in check",
                        subtitle = "Set one monthly budget and the home dashboard will track how much is left."
                    )
                    uiState.existingAmountMinor?.let { existing ->
                        Text(
                            text = "Current monthly budget: ${formatAmount(existing)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.secondary
                ) {
                    OutlinedTextField(
                        value = uiState.budgetName,
                        onValueChange = viewModel::updateBudgetName,
                        label = { Text("Budget Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = viewModel::updateAmount,
                        label = { Text("Monthly Amount") },
                        leadingIcon = { Text("\u20B9") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::saveBudget,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .appButtonSizing()
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null)
                    Text(
                        text = if (uiState.isSaving) "Saving..." else "Save Monthly Budget",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (uiState.existingAmountMinor != null) {
                item {
                    OutlinedButton(
                        onClick = viewModel::removeBudget,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .appButtonSizing()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Text(
                            text = "Remove Budget",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
