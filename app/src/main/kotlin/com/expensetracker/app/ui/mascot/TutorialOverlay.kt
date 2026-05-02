package com.expensetracker.app.ui.mascot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TutorialTarget {
    MonthlySummary,
    QuickActions,
    AddButton,
    BottomBar
}

private data class TutorialStep(
    val mood: PiggyMood,
    val assetSet: PennyAssetSet,
    val line: String,
    val target: TutorialTarget?,
    val nextLabel: String,
)

private val tutorialSteps = listOf(
    TutorialStep(
        mood = PiggyMood.Wave,
        assetSet = PennyAssetSet.HalfCut,
        line = "Hi! I'm Penny, your money buddy. Let me show you around in 30 seconds.",
        target = null,
        nextLabel = "Sounds good"
    ),
    TutorialStep(
        mood = PiggyMood.Curious,
        assetSet = PennyAssetSet.HalfCut,
        line = "This is your monthly snapshot: spent, earned, pacing, and what is left for the month.",
        target = TutorialTarget.MonthlySummary,
        nextLabel = "Got it"
    ),
    TutorialStep(
        mood = PiggyMood.Excited,
        assetSet = PennyAssetSet.HalfCut,
        line = "These shortcuts open budgets, smart capture, statements, and manual entry without digging through menus.",
        target = TutorialTarget.QuickActions,
        nextLabel = "Nice"
    ),
    TutorialStep(
        mood = PiggyMood.Coin,
        assetSet = PennyAssetSet.Full,
        line = "Use the plus button when you want to add a transaction yourself.",
        target = TutorialTarget.AddButton,
        nextLabel = "Cool"
    ),
    TutorialStep(
        mood = PiggyMood.Wink,
        assetSet = PennyAssetSet.HalfCut,
        line = "The bottom tabs take you between Home, Ledger, Calendar, Analytics, and Settings.",
        target = TutorialTarget.BottomBar,
        nextLabel = "Okay"
    ),
    TutorialStep(
        mood = PiggyMood.Cheer,
        assetSet = PennyAssetSet.HalfCut,
        line = "That's the tour. You can replay it from Settings any time.",
        target = null,
        nextLabel = "Let's go"
    ),
)

@Composable
fun TutorialOverlay(
    onFinish: () -> Unit,
    targetBounds: Map<TutorialTarget, Rect> = emptyMap(),
) {
    var step by remember { mutableStateOf(0) }
    val current = tutorialSteps[step]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val spotlightRect = spotlightRect(current.target, targetBounds, widthPx, heightPx)
        val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 20.dp
        val maxBubbleWidth = minOf(maxWidth - horizontalPadding - horizontalPadding, 340.dp)
        val placement = tutorialPlacement(current.target, spotlightRect, widthPx, heightPx)

        SpotlightScrim(rect = spotlightRect)
        if (spotlightRect != null) {
            SpotlightPulse(rect = spotlightRect)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = horizontalPadding, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tutorialSteps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == step) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == step) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }
            TextButton(onClick = onFinish) {
                Text(
                    "Skip",
                    color = Color.White.copy(alpha = 0.88f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .align(placement.alignment)
                .padding(placement.padding)
                .padding(horizontal = horizontalPadding)
                .widthIn(max = maxBubbleWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    (slideInVertically { it / 4 } + fadeIn(tween(380))) togetherWith
                        (slideOutVertically { -it / 6 } + fadeOut(tween(180)))
                },
                label = "penny-tour"
            ) { stepNow ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpeechBubble(
                        text = stepNow.line,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Piggy(
                        mood = stepNow.mood,
                        size = if (stepNow.assetSet == PennyAssetSet.Full) 132.dp else 122.dp,
                        assetSet = stepNow.assetSet
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (step < tutorialSteps.lastIndex) step++ else onFinish()
                },
                modifier = Modifier
                    .widthIn(min = 168.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(current.nextLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SpotlightScrim(rect: Rect?) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRect(Color.Black.copy(alpha = 0.78f))
        rect ?: return@Canvas
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
            blendMode = BlendMode.Clear
        )
    }
}

@Composable
private fun SpotlightPulse(rect: Rect) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grow = 12f * pulse
        drawRoundRect(
            color = Color(0xFF00E5A0).copy(alpha = (1f - pulse) * 0.7f),
            topLeft = Offset(rect.left - grow, rect.top - grow),
            size = Size(rect.width + grow * 2, rect.height + grow * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f + grow, 28f + grow),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
        drawRoundRect(
            color = Color(0xFF00E5A0).copy(alpha = 0.9f),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}

private data class TutorialPlacement(
    val alignment: Alignment,
    val padding: PaddingValues,
)

private fun tutorialPlacement(
    target: TutorialTarget?,
    rect: Rect?,
    width: Float,
    height: Float,
): TutorialPlacement {
    val alignment = when {
        target == null || rect == null -> Alignment.Center
        target == TutorialTarget.MonthlySummary -> Alignment.BottomCenter
        rect.center.y < height * 0.56f -> Alignment.BottomCenter
        rect.center.y > height * 0.60f -> Alignment.TopCenter
        rect.center.x > width * 0.55f -> Alignment.CenterStart
        else -> Alignment.CenterEnd
    }
    val top = when (alignment) {
        Alignment.TopCenter -> 76.dp
        Alignment.Center -> 56.dp
        else -> 0.dp
    }
    val bottom = when (alignment) {
        Alignment.BottomCenter -> 108.dp
        Alignment.Center -> 24.dp
        else -> 24.dp
    }
    return TutorialPlacement(
        alignment = alignment,
        padding = PaddingValues(top = top, bottom = bottom)
    )
}

private fun spotlightRect(
    target: TutorialTarget?,
    targetBounds: Map<TutorialTarget, Rect>,
    width: Float,
    height: Float,
): Rect? {
    target ?: return null
    val measured = targetBounds[target]
        ?.takeIf { it.width > 1f && it.height > 1f }
        ?.let { it.expandedBy(12f, width, height) }
    return measured ?: fallbackRect(target, width, height)
}

private fun Rect.expandedBy(padding: Float, maxWidth: Float, maxHeight: Float): Rect =
    Rect(
        left = (left - padding).coerceAtLeast(0f),
        top = (top - padding).coerceAtLeast(0f),
        right = (right + padding).coerceAtMost(maxWidth),
        bottom = (bottom + padding).coerceAtMost(maxHeight)
    )

private fun fallbackRect(target: TutorialTarget, width: Float, height: Float): Rect =
    when (target) {
        TutorialTarget.MonthlySummary -> Rect(
            left = width * 0.04f,
            top = height * 0.10f,
            right = width * 0.96f,
            bottom = height * 0.58f
        )
        TutorialTarget.QuickActions -> Rect(
            left = width * 0.04f,
            top = height * 0.56f,
            right = width * 0.96f,
            bottom = height * 0.80f
        )
        TutorialTarget.AddButton -> Rect(
            left = width * 0.76f,
            top = height * 0.74f,
            right = width * 0.96f,
            bottom = height * 0.88f
        )
        TutorialTarget.BottomBar -> Rect(
            left = width * 0.04f,
            top = height * 0.86f,
            right = width * 0.96f,
            bottom = height * 0.99f
        )
    }

@Composable
private fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.97f))
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1B0F1F),
            textAlign = TextAlign.Center
        )
    }
}
