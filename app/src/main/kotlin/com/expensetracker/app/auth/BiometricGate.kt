package com.expensetracker.app.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.prefs.UserPreferences
import com.expensetracker.app.ui.theme.AuroraBackground
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BiometricViewModel @Inject constructor(
    userPreferences: UserPreferences
) : ViewModel() {
    val biometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    // Only relock if the app was backgrounded for longer than this. Using a
    // short Android picker / share sheet / biometric prompt should NOT relock.
    private val relockGraceMillis: Long = 30_000L
    private var stoppedAt: Long = 0L

    fun markUnlocked() { _unlocked.value = true }

    fun onStop() {
        stoppedAt = System.currentTimeMillis()
    }

    fun onStart() {
        val stopped = stoppedAt
        if (stopped != 0L && System.currentTimeMillis() - stopped > relockGraceMillis) {
            _unlocked.value = false
        }
        stoppedAt = 0L
    }
}

@Composable
fun BiometricGate(
    content: @Composable () -> Unit
) {
    val viewModel: BiometricViewModel = hiltViewModel()
    val enabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (!enabled) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                Lifecycle.Event.ON_START -> viewModel.onStart()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldGate = enabled && !unlocked
    val canAuthenticate = remember(enabled) {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(shouldGate, canAuthenticate) {
        if (shouldGate && canAuthenticate) {
            val activity = context as? FragmentActivity ?: return@LaunchedEffect
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.markUnlocked()
                }
            })
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Pocket Pulse")
                .setSubtitle("Confirm it's you")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
            prompt.authenticate(info)
        } else if (shouldGate && !canAuthenticate) {
            viewModel.markUnlocked()
        }
    }

    if (shouldGate) {
        LockScreen(onUnlockClick = {
            val activity = context as? FragmentActivity ?: return@LockScreen
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.markUnlocked()
                }
            })
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Pocket Pulse")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
            prompt.authenticate(info)
        })
    } else {
        content()
    }
}

@Composable
private fun LockScreen(onUnlockClick: () -> Unit) {
    AuroraBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Fingerprint, null,
                        tint = Color.White, modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text("Locked", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onUnlockClick) { Text("Unlock") }
            }
        }
    }
}
