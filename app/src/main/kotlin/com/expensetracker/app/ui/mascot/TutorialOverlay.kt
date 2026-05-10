package com.expensetracker.app.ui.mascot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TutorialTarget {
    MonthlySummary,
    QuickActions,
    AddButton,
    BottomBar
}

private data class TutorialStep(
    val mood: PiggyMood,
    val assetSet: PennyAssetSet,
    val title: String,
    val line: String,
    val target: TutorialTarget?,
    val nextLabel: String,
)

private val tutorialSteps = listOf(
    TutorialStep(
        mood = PiggyMood.Wave,
        assetSet = PennyAssetSet.HalfCut,
        title = "Hey there!",
        line = "I'm Penny, your money buddy. Let me show you around in 30 seconds.",
        target = null,
        nextLabel = "Sounds good"
    ),
    TutorialStep(
        mood = PiggyMood.Curious,
        assetSet = PennyAssetSet.HalfCut,
        title = "Your month at a glance",
        line = "Spent, earned, pacing, and what's left for the month — all right here.",
        target = TutorialTarget.MonthlySummary,
        nextLabel = "Got it"
    ),
    TutorialStep(
        mood = PiggyMood.Excited,
        assetSet = PennyAssetSet.HalfCut,
        title = "One-tap shortcuts",
        line = "Open budgets, smart capture, statements, and manual entry without digging through menus.",
        target = TutorialTarget.QuickActions,
        nextLabel = "Nice"
    ),
    TutorialStep(
        mood = PiggyMood.Coin,
        assetSet = PennyAssetSet.Full,
        title = "Add it yourself",
        line = "Tap the plus button whenever you want to log a transaction by hand.",
        target = TutorialTarget.AddButton,
        nextLabel = "Cool"
    ),
    TutorialStep(
        mood = PiggyMood.Wink,
        assetSet = PennyAssetSet.HalfCut,
        title = "Find your way",
        line = "The bottom tabs hop between Home, Ledger, Calendar, Analytics, and Settings.",
        target = TutorialTarget.BottomBar,
        nextLabel = "Okay"
    ),
    TutorialStep(
        mood = PiggyMood.Cheer,
        assetSet = PennyAssetSet.HalfCut,
        title = "All set!",
        line = "That's the tour. You can replay it from Settings any time.",
        target = null,
        nextLabel = "Let's go"
    ),
)

private val MintAccent = Color(0xFF00E5A0)
private val MintAccentDeep = Color(0xFF00B786)
private val BubbleTop = Color(0xFFFFFFFF)
private val BubbleBottom = Color(0xFFEAFFF6)
private val InkPrimary = Color(0xFF0F1F1A)
private val InkMuted = Color(0xFF4A6B5F)

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
        val maxBubbleWidth = minOf(maxWidth - horizontalPadding - horizontalPadding, 360.dp)
        val placement = tutorialPlacement(current.target, spotlightRect, widthPx, heightPx)

        SpotlightScrim(rect = spotlightRect)
        if (spotlightRect != null) {
            SpotlightHalo(rect = spotlightRect)
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
            StepDots(total = tutorialSteps.size, current = step)
            SkipPill(onClick = onFinish)
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
                        title = stepNow.title,
                        text = stepNow.line,
                        tailDown = placement.bubbleTailDown,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(if (placement.bubbleTailDown) 14.dp else 8.dp))
                    Piggy(
                        mood = stepNow.mood,
                        size = if (stepNow.assetSet == PennyAssetSet.Full) 138.dp else 112.dp,
                        assetSet = stepNow.assetSet
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            NextButton(label = current.nextLabel, isLast = step == tutorialSteps.lastIndex) {
                if (step < tutorialSteps.lastIndex) step++ else onFinish()
            }
        }
    }
}

@Composable
private fun StepDots(total: Int, current: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .size(width = if (active) 22.dp else 7.dp, height = 7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) Brush.horizontalGradient(listOf(MintAccent, MintAccentDeep))
                        else Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.35f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun SkipPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Skip",
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun NextButton(label: String, isLast: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = MintAccent,
                spotColor = MintAccent
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    if (isLast) listOf(Color(0xFF00F0B2), Color(0xFF00B786))
                    else listOf(MintAccent, MintAccentDeep)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Text(
            label,
            color = Color(0xFF052017),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun SpotlightScrim(rect: Rect?) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        // Rich vertical-gradient scrim instead of flat black for more depth.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF06120E).copy(alpha = 0.86f),
                    Color(0xFF0A1F18).copy(alpha = 0.82f),
                    Color(0xFF06120E).copy(alpha = 0.88f)
                )
            )
        )
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
private fun SpotlightHalo(rect: Rect) {
    // Soft radial glow around the spotlight to draw the eye in.
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = rect.center.x
        val cy = rect.center.y
        val radius = (maxOf(rect.width, rect.height) * 0.85f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    MintAccent.copy(alpha = 0.22f),
                    MintAccent.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            center = Offset(cx, cy),
            radius = radius
        )
    }
}

@Composable
private fun SpotlightPulse(rect: Rect) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val pulse1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Outer pulses (two rings, phase-offset)
        listOf(pulse1, pulse2).forEach { p ->
            val grow = 22f * p
            drawRoundRect(
                color = MintAccent.copy(alpha = (1f - p) * 0.55f),
                topLeft = Offset(rect.left - grow, rect.top - grow),
                size = Size(rect.width + grow * 2, rect.height + grow * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f + grow, 28f + grow),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.4f)
            )
        }
        // Crisp inner outline with gradient stroke
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(MintAccent, Color(0xFF7CFFD4), MintAccentDeep),
                start = Offset(rect.left, rect.top),
                end = Offset(rect.right, rect.bottom)
            ),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
        )
        // Soft inner glow tracing the inside edge
        drawRoundRect(
            color = MintAccent.copy(alpha = 0.25f),
            topLeft = Offset(rect.left + 3f, rect.top + 3f),
            size = Size(rect.width - 6f, rect.height - 6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(26f, 26f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}

private data class TutorialPlacement(
    val alignment: Alignment,
    val padding: PaddingValues,
    val bubbleTailDown: Boolean,
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
        Alignment.BottomCenter -> 116.dp
        Alignment.Center -> 24.dp
        else -> 24.dp
    }
    // Tail points DOWN (toward the spotlight) when the bubble sits ABOVE its target.
    val tailDown = alignment == Alignment.BottomCenter
    return TutorialPlacement(
        alignment = alignment,
        padding = PaddingValues(top = top, bottom = bottom),
        bubbleTailDown = tailDown
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
    title: String,
    text: String,
    tailDown: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = MintAccent,
                    spotColor = MintAccent.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BubbleTop, BubbleBottom)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MintAccent.copy(alpha = 0.55f),
                            MintAccent.copy(alpha = 0.18f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // Top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(MintAccent, Color(0xFF7CFFD4), MintAccentDeep)
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = 18.dp,
                    bottom = 18.dp
                ),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MintAccentDeep)
                    )
                    Text(
                        text = "PENNY SAYS",
                        color = MintAccentDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    color = InkPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
                )
            }
        }
        if (tailDown) {
            BubbleTail(pointingDown = true)
        }
    }
}

@Composable
private fun BubbleTail(pointingDown: Boolean) {
    // A small triangle anchored to the bottom of the bubble.
    Canvas(
        modifier = Modifier
            .offset(y = (-1).dp)
            .size(width = 22.dp, height = 12.dp)
    ) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (pointingDown) {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w / 2f, h)
                close()
            } else {
                moveTo(w / 2f, 0f)
                lineTo(0f, h)
                lineTo(w, h)
                close()
            }
        }
        drawPath(path = path, color = BubbleBottom)
        drawPath(
            path = path,
            color = MintAccent.copy(alpha = 0.45f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }
}
