package com.expensetracker.app.ai

import android.content.Context
import com.expensetracker.app.core.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1 placeholder. Reports compatibility + the persisted model state so the
 * Settings screen can render the AI section, but all lifecycle methods short-
 * circuit until Phase 3 (download) and Phase 4 (runtime) replace this.
 */
@Singleton
class StubOnDeviceAiManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) : OnDeviceAiManager {

    override fun observeAvailability(): Flow<AiAvailability> =
        userPreferences.aiModelState.map { state ->
            when (val compat = AiCompatibility.evaluate(DeviceInfoProvider.current(context))) {
                is AiCompatibility.Result.Unsupported -> AiAvailability.Unsupported(compat.reasons)
                AiCompatibility.Result.Supported -> when {
                    state.lastError.isNotEmpty() -> AiAvailability.Error(state.lastError)
                    state.version.isEmpty() -> AiAvailability.NeedsDownload
                    else -> AiAvailability.Ready
                }
            }
        }

    override suspend fun downloadModel(): Result<Unit> =
        Result.failure(NotImplementedError("Model download is wired in phase 3"))

    override suspend fun deleteModel() {
        userPreferences.clearAiModelState()
    }

    override suspend fun initializeIfNeeded(): Result<Unit> =
        Result.failure(NotImplementedError("Runtime is wired in phase 4"))

    override suspend fun createConversation(): AiConversation =
        error("AI runtime is not available yet")
}
