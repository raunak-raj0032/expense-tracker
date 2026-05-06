package com.expensetracker.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Layout tokens ────────────────────────────────────────────────────────────
val ScreenEdgePadding  = 20.dp
val AppButtonMinHeight = 52.dp
val CardSpacing        = 16.dp
val SectionSpacing     = 24.dp
val ItemSpacing        = 10.dp

fun Modifier.appButtonSizing(): Modifier = defaultMinSize(minHeight = AppButtonMinHeight)

data class AdaptiveFlowLayout(
    val columns: Int,
    val itemFraction: Float
)

fun TextStyle.financialFigures(
    weight: FontWeight? = null,
    letterSpacing: TextUnit = (-0.5).sp
): TextStyle = copy(
    fontWeight       = weight ?: this.fontWeight,
    letterSpacing    = letterSpacing,
    fontFeatureSettings = "tnum"
)

fun adaptiveFlowLayout(
    maxWidth: Dp,
    minItemWidth: Dp,
    spacing: Dp,
    maxColumns: Int
): AdaptiveFlowLayout {
    if (maxWidth <= 0.dp) return AdaptiveFlowLayout(1, 1f)
    val cols = maxColumns.coerceAtLeast(1)
    var columns = cols
    while (columns > 1) {
        val candidateWidth = (maxWidth - spacing * (columns - 1)) / columns
        if (candidateWidth >= minItemWidth) break
        columns -= 1
    }
    val itemWidth = (maxWidth - spacing * (columns - 1)) / columns
    return AdaptiveFlowLayout(
        columns      = columns,
        itemFraction = (itemWidth / maxWidth).coerceIn(0f, 1f)
    )
}

// ─── Animated Aurora Background ───────────────────────────────────────────────
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val blob1X by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue  = 40f,
        animationSpec = infiniteRepeatable(
            animation  = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1x"
    )
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue  = 20f,
        animationSpec = infiniteRepeatable(
            animation  = tween(11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1y"
    )
    val blob2X by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue  = -20f,
        animationSpec = infiniteRepeatable(
            animation  = tween(13000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2x"
    )
    val blob3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue  = 0.22f,
        animationSpec = infiniteRepeatable(
            animation  = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3a"
    )

    val background = MaterialTheme.colorScheme.background
    val primary    = MaterialTheme.colorScheme.primary
    val secondary  = MaterialTheme.colorScheme.secondary
    val tertiary   = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Blob 1 — mint, top-left, drifting
        Box(
            modifier = Modifier
                .offset(x = blob1X.dp, y = blob1Y.dp)
                .size(320.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Blob 2 — cyan, top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = blob2X.dp, y = (-40).dp)
                .size(260.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Blob 3 — amber, bottom, pulsing
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 60.dp)
                .size(280.dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tertiary.copy(alpha = blob3Alpha),
                            Color.Transparent
                        )
                    )
                )
        )
        // Subtle grid-noise overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            background.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

// ─── Premium Glass Panel ──────────────────────────────────────────────────────
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.45f),
                            accent.copy(alpha = 0.08f),
                            surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                ),
                shape = MaterialTheme.shapes.large
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        surface.copy(alpha = 0.92f),
                        surfaceVariant.copy(alpha = 0.88f)
                    )
                )
            )
    ) {
        // Subtle top-edge shine
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Corner accent glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────
@Composable
fun SectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text  = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(
                    text  = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing
            )
        }
    }
}

// ─── Stat Badge ───────────────────────────────────────────────────────────────
@Composable
fun StatBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 88.dp, minHeight = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                RoundedCornerShape(16.dp)
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text  = value,
                style = MaterialTheme.typography.titleSmall.financialFigures(FontWeight.Bold)
            )
        }
    }
}

// ─── Animated Glow Progress Bar ───────────────────────────────────────────────
@Composable
fun GlowProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 6.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue  = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "glowProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glowShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(height)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.8f),
                                accent,
                                accent.copy(alpha = shimmerAlpha)
                            )
                        )
                    )
            )
        }
    }
}

// ─── Neon Pill ────────────────────────────────────────────────────────────────
@Composable
fun NeonPill(
    text: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.4f)), CircleShape)
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── App Top Bar ──────────────────────────────────────────────────────────────
/**
 * Branded top bar for detail screens with a back button.
 * Shows a styled back arrow, title with optional eyebrow/subtitle, and
 * a thin accent shimmer line at the bottom.
 */
@Composable
fun DetailTopBar(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    onNavigateBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val surface        = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(surface.copy(alpha = 0.88f), surfaceVariant.copy(alpha = 0.60f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Styled back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.10f))
                        .clickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // Title block
                Column(
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (eyebrow != null) {
                        Text(
                            text = eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
        TopBarShimmerLine(accent)
    }
}

/**
 * Branded top bar for main (tab) screens — no back button.
 * Shows a dot accent + bold title and optional subtitle, with a shimmer bottom line.
 */
@Composable
fun MainTopBar(
    title: String,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val surface        = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(surface.copy(alpha = 0.88f), surfaceVariant.copy(alpha = 0.60f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(accent, accent.copy(alpha = 0.4f))
                                    )
                                )
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
        TopBarShimmerLine(accent)
    }
}

@Composable
private fun TopBarShimmerLine(accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.55f),
                        accent.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                )
            )
    )
}

// ─── Accent Divider ───────────────────────────────────────────────────────────
@Composable
fun AccentDivider(
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
    )
}
