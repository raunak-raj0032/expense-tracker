package com.expensetracker.app.ui.mascot

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PiggyMood {
    Happy, Wave, Cheer, Wink, Sleepy, Curious, Coin, Love, Excited, Surprised
}

enum class PennyAssetSet {
    Auto, Full, HalfCut
}

@Composable
fun Piggy(
    modifier: Modifier = Modifier,
    mood: PiggyMood = PiggyMood.Happy,
    size: Dp = 160.dp,
    animate: Boolean = true,
    assetSet: PennyAssetSet = PennyAssetSet.Auto,
) {
    val transition = rememberInfiniteTransition(label = "penny")
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "penny-bob"
    )
    val wiggle by transition.animateFloat(
        initialValue = -pennyWiggleMax(mood),
        targetValue = pennyWiggleMax(mood),
        animationSpec = infiniteRepeatable(
            animation = tween(pennyWiggleDuration(mood), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "penny-wiggle"
    )
    val bounce by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (mood == PiggyMood.Cheer) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "penny-bounce"
    )

    val assetPath = remember(mood, assetSet) { pennyAssetPath(mood, assetSet) }
    val image = rememberAssetImage(assetPath)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = "Penny mascot",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = if (animate) bob * 4f else 0f
                        rotationZ = if (animate) wiggle else 0f
                        scaleX = if (animate) bounce else 1f
                        scaleY = if (animate) bounce else 1f
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun rememberAssetImage(assetPath: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(context, assetPath) {
        runCatching {
            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

private fun pennyAssetPath(mood: PiggyMood, assetSet: PennyAssetSet): String =
    when (assetSet) {
        PennyAssetSet.Full -> fullAssetPath(mood) ?: DEFAULT_MASCOT
        PennyAssetSet.HalfCut -> halfCutAssetPath(mood) ?: fullAssetPath(mood) ?: DEFAULT_MASCOT
        PennyAssetSet.Auto -> when (mood) {
            PiggyMood.Happy -> DEFAULT_MASCOT
            PiggyMood.Wave,
            PiggyMood.Cheer,
            PiggyMood.Wink,
            PiggyMood.Coin -> fullAssetPath(mood) ?: DEFAULT_MASCOT
            PiggyMood.Sleepy,
            PiggyMood.Curious,
            PiggyMood.Love,
            PiggyMood.Excited,
            PiggyMood.Surprised -> halfCutAssetPath(mood) ?: DEFAULT_MASCOT
        }
    }

private fun fullAssetPath(mood: PiggyMood): String? =
    when (mood) {
        PiggyMood.Happy -> "mascot/emotions/full/penny_happy.png"
        PiggyMood.Wave -> "mascot/emotions/full/penny_wave.png"
        PiggyMood.Cheer -> "mascot/emotions/full/penny_cheer.png"
        PiggyMood.Wink -> "mascot/emotions/full/penny_wink.png"
        PiggyMood.Coin -> "mascot/emotions/full/penny_coin.png"
        PiggyMood.Sleepy -> "mascot/emotions/full/penny_sleepy.png"
        PiggyMood.Curious -> "mascot/emotions/full/penny_curious.png"
        PiggyMood.Love -> "mascot/emotions/full/penny_love.png"
        PiggyMood.Excited -> "mascot/emotions/full/penny_excited.png"
        PiggyMood.Surprised -> "mascot/emotions/full/penny_surprised.png"
    }

private fun halfCutAssetPath(mood: PiggyMood): String? =
    when (mood) {
        PiggyMood.Happy -> "mascot/emotions/half_cut/penny_cheer.png"
        PiggyMood.Wave -> "mascot/emotions/half_cut/penny_wave.png"
        PiggyMood.Cheer -> "mascot/emotions/half_cut/penny_cheer.png"
        PiggyMood.Wink -> "mascot/emotions/half_cut/penny_wink.png"
        PiggyMood.Sleepy -> "mascot/emotions/half_cut/penny_sleepy.png"
        PiggyMood.Curious -> "mascot/emotions/half_cut/penny_curious.png"
        PiggyMood.Love -> "mascot/emotions/half_cut/penny_love.png"
        PiggyMood.Excited -> "mascot/emotions/half_cut/penny_excited.png"
        PiggyMood.Surprised -> "mascot/emotions/half_cut/penny_surprised.png"
        PiggyMood.Coin -> null
    }

private fun pennyWiggleMax(mood: PiggyMood): Float =
    when (mood) {
        PiggyMood.Wave -> 5f
        PiggyMood.Cheer,
        PiggyMood.Excited -> 3.5f
        PiggyMood.Curious -> 2.5f
        PiggyMood.Love -> 2f
        else -> 1.2f
    }

private fun pennyWiggleDuration(mood: PiggyMood): Int =
    when (mood) {
        PiggyMood.Wave -> 760
        PiggyMood.Cheer,
        PiggyMood.Excited -> 520
        PiggyMood.Curious -> 1800
        else -> 2400
    }

private const val DEFAULT_MASCOT = "mascot/penny_mascot.png"
