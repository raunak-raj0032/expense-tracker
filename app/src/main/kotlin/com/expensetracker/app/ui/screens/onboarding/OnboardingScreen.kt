@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.expensetracker.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.prefs.UserPreferences
import com.expensetracker.app.ui.mascot.PennyAssetSet
import com.expensetracker.app.ui.mascot.Piggy
import com.expensetracker.app.ui.mascot.PiggyMood
import com.expensetracker.app.ui.theme.AuroraBackground
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    fun complete() {
        viewModelScope.launch { userPreferences.setOnboardingSeen(true) }
    }
}

private data class Slide(
    val mood: PiggyMood,
    val assetSet: PennyAssetSet,
    val line: String,
    val title: String,
    val body: String,
)

private val slides = listOf(
    Slide(
        mood = PiggyMood.Happy,
        assetSet = PennyAssetSet.Auto,
        line = "Hi! I'm Penny — your money buddy.",
        title = "Meet Penny",
        body = "I'll keep an eye on every rupee that comes and goes, so you don't have to."
    ),
    Slide(
        mood = PiggyMood.Coin,
        assetSet = PennyAssetSet.Full,
        line = "Got a payment SMS? *gulp* — straight in.",
        title = "Capture on autopilot",
        body = "SMS, notifications and PDF statements turn into transactions automatically. No typing required."
    ),
    Slide(
        mood = PiggyMood.Curious,
        assetSet = PennyAssetSet.HalfCut,
        line = "Where's your money going? Let's find out.",
        title = "See the bigger picture",
        body = "Budgets, calendar heatmaps and category insights — your spending finally makes sense."
    ),
    Slide(
        mood = PiggyMood.Love,
        assetSet = PennyAssetSet.HalfCut,
        line = "Your secrets are safe with me. Pinky promise!",
        title = "Private by default",
        body = "Everything stays on this device. No clouds. No prying eyes. Just you and me."
    ),
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    val finish: () -> Unit = {
        viewModel.complete()
        onFinish()
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ScreenEdgePadding, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = finish) { Text("Skip") }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                SlideContent(slides[page])
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slides.forEachIndexed { i, _ ->
                        val selected = pagerState.currentPage == i
                        val scale by animateFloatAsState(if (selected) 1.2f else 1f, label = "dot")
                        Box(
                            modifier = Modifier
                                .size(if (selected) 10.dp else 8.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < slides.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else finish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when (pagerState.currentPage) {
                            0 -> "Nice to meet you, Penny"
                            slides.lastIndex -> "Let's go!"
                            else -> "Tell me more"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideContent(slide: Slide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Piggy(mood = slide.mood, size = 200.dp, assetSet = slide.assetSet)

        Spacer(Modifier.height(20.dp))

        // Penny's voice — small chat bubble that fades between slides
        AnimatedContent(
            targetState = slide.line,
            transitionSpec = {
                (slideInVertically { it / 3 } + fadeIn(tween(420))) togetherWith
                    (slideOutVertically { -it / 3 } + fadeOut(tween(220)))
            },
            label = "penny-line"
        ) { line ->
            SpeechBubble(line)
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpeechBubble(text: String) {
    Box(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
