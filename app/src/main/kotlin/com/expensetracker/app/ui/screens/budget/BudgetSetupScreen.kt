package com.expensetracker.app.ui.screens.budget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.AccentDivider
import com.expensetracker.app.ui.theme.DetailTopBar
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.GlowProgressBar
import com.expensetracker.app.ui.theme.NeonPill
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.appButtonSizing
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private const val SLIDER_MIN = 1_000f
private const val SLIDER_MAX = 3_00_000f
private val PRESETS = listOf(5_000L, 10_000L, 25_000L, 50_000L, 1_00_000L, 2_00_000L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val sliderValue = uiState.amount.toFloatOrNull()?.coerceIn(SLIDER_MIN, SLIDER_MAX) ?: SLIDER_MIN

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
                start  = ScreenEdgePadding,
                top    = 8.dp,
                end    = ScreenEdgePadding,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BudgetHeroCard(
                    amount              = uiState.amount,
                    existingAmountMinor = uiState.existingAmountMinor
                )
            }

            item {
                GlassPanel(
                    modifier       = Modifier.fillMaxWidth(),
                    accent         = MaterialTheme.colorScheme.primary,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // ── Slider + presets ───────────────────────────────────
                    BudgetSlider(
                        value         = sliderValue,
                        onValueChange = { viewModel.updateAmount(it.roundToInt().toString()) }
                    )
                    PresetChips(
                        onSelect       = { viewModel.updateAmount(it.toString()) },
                        selectedRupees = uiState.amount.toDoubleOrNull()?.toLong()
                    )

                    // ── Thin divider ───────────────────────────────────────
                    AccentDivider(accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))

                    // ── Name + amount fields side by side ──────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value         = uiState.budgetName,
                            onValueChange = viewModel::updateBudgetName,
                            label         = { Text("Name") },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            colors        = budgetFieldColors()
                        )
                        OutlinedTextField(
                            value           = uiState.amount,
                            onValueChange   = viewModel::updateAmount,
                            label           = { Text("Amount") },
                            leadingIcon     = { Text("₹", fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine      = true,
                            modifier        = Modifier.weight(1f),
                            colors          = budgetFieldColors()
                        )
                    }

                    uiState.error?.let { error ->
                        Text(
                            text       = error,
                            style      = MaterialTheme.typography.bodySmall,
                            color      = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // ── Action buttons ─────────────────────────────────────
                    if (uiState.existingAmountMinor != null) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GradientSaveButton(
                                isSaving = uiState.isSaving,
                                onClick  = viewModel::saveBudget,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick  = viewModel::removeBudget,
                                enabled  = !uiState.isSaving,
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        GradientSaveButton(
                            isSaving = uiState.isSaving,
                            onClick  = viewModel::saveBudget,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetHeroCard(amount: String, existingAmountMinor: Long?) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.90f), secondary.copy(alpha = 0.75f))
                )
            )
            .padding(24.dp)
    ) {
        // Decorative glow in top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(130.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                NeonPill(
                    text   = if (existingAmountMinor != null) "Editing" else "New Budget",
                    accent = Color.White.copy(alpha = 0.85f)
                )
                Surface(
                    color        = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    shape        = CircleShape
                ) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp)
                    )
                }
            }

            Text(
                text          = "Monthly Target",
                style         = MaterialTheme.typography.labelMedium,
                color         = Color.White.copy(alpha = 0.72f),
                letterSpacing = 1.sp
            )

            val displayAmount = amount.toDoubleOrNull()
                ?.let { "₹${formatWithCommas(it.toLong())}" }
                ?: "₹ —"

            Text(
                text       = displayAmount,
                style      = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )

            existingAmountMinor?.let {
                AccentDivider(accent = Color.White.copy(alpha = 0.25f))
                Text(
                    text  = "Current cap: ${formatAmount(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun BudgetSlider(value: Float, onValueChange: (Float) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val fraction = ((value - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN)).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = "₹1K",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = "₹3L",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value        = value,
            onValueChange = onValueChange,
            valueRange   = SLIDER_MIN..SLIDER_MAX,
            modifier     = Modifier.fillMaxWidth(),
            colors       = SliderDefaults.colors(
                thumbColor          = primary,
                activeTrackColor    = primary,
                inactiveTrackColor  = primary.copy(alpha = 0.18f)
            )
        )

        GlowProgressBar(progress = fraction, accent = primary, height = 3.dp)
    }
}

@Composable
private fun PresetChips(onSelect: (Long) -> Unit, selectedRupees: Long?) {
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PRESETS.forEach { preset ->
            val isSelected = selectedRupees == preset
            val bgColor by animateColorAsState(
                targetValue   = if (isSelected) primary else primary.copy(alpha = 0.08f),
                animationSpec = tween(200),
                label         = "chip_bg_$preset"
            )
            val contentColor by animateColorAsState(
                targetValue   = if (isSelected) MaterialTheme.colorScheme.onPrimary else primary,
                animationSpec = tween(200),
                label         = "chip_text_$preset"
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else primary.copy(alpha = 0.30f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(preset) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = contentColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text       = "₹${formatPreset(preset)}",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientSaveButton(isSaving: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val alphaMod  = if (isSaving) 0.45f else 1f

    Box(
        modifier = modifier
            .appButtonSizing()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = alphaMod),
                        secondary.copy(alpha = alphaMod)
                    )
                )
            )
            .clickable(enabled = !isSaving, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color       = Color.White.copy(alpha = 0.8f)
                )
            } else {
                Icon(Icons.Default.Savings, contentDescription = null, tint = Color.White)
            }
            Text(
                text       = if (isSaving) "Saving…" else "Save Monthly Budget",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }
}

private fun formatWithCommas(amount: Long): String =
    NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)

private fun formatPreset(rupees: Long): String = when {
    rupees >= 1_00_000 -> "${rupees / 1_00_000}L"
    rupees >= 1_000    -> "${rupees / 1_000}K"
    else               -> rupees.toString()
}

@Composable
private fun budgetFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor  = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
    focusedBorderColor    = MaterialTheme.colorScheme.secondary,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
    focusedContainerColor   = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f)
)
