package com.expensetracker.app.ai

import android.content.Context
import com.expensetracker.app.core.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSlmAiManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val modelStore: LocalModelStore,
    private val downloader: LocalModelDownloader
) : OnDeviceAiManager {

    private val downloadState = MutableStateFlow<AiAvailability.Downloading?>(null)
    private val generationMutex = Mutex()

    override fun observeAvailability(): Flow<AiAvailability> =
        combine(userPreferences.aiModelState, downloadState) { state, downloading ->
            val compat = AiCompatibility.evaluate(DeviceInfoProvider.current(context))
            when {
                compat is AiCompatibility.Result.Unsupported -> AiAvailability.Unsupported(compat.reasons)
                downloading != null -> downloading
                state.lastError.isNotBlank() -> AiAvailability.Error(state.lastError)
                modelStore.isInstalled(state) -> AiAvailability.Ready
                else -> AiAvailability.NeedsDownload
            }
        }

    override suspend fun downloadModel(): Result<Unit> = runCatching {
        val compat = AiCompatibility.evaluate(DeviceInfoProvider.current(context))
        if (compat is AiCompatibility.Result.Unsupported) {
            error("This device does not meet the local AI requirements.")
        }
        val temp = modelStore.prepareTempFile()
        val token = userPreferences.hfToken.first()
        downloadState.value = AiAvailability.Downloading(0L, AiModelSpec.DISPLAY_SIZE_BYTES)
        downloader.download(temp, token) { downloaded, total ->
            downloadState.value = AiAvailability.Downloading(downloaded, total)
        }
        val installed = modelStore.installVerifiedTemp()
        userPreferences.setLocalAiModelInstalled(AiModelSpec.VERSION, installed.absolutePath)
    }.onFailure {
        modelStore.deleteAll()
        userPreferences.setAiLastError(it.message ?: "Model download failed.")
    }.also {
        downloadState.value = null
    }

    override suspend fun deleteModel() {
        modelStore.deleteAll()
        userPreferences.clearLocalAiModelState()
    }

    override suspend fun initializeIfNeeded(): Result<Unit> = runCatching {
        val state = userPreferences.aiModelState.first()
        if (!modelStore.isInstalled(state)) error("Download the local AI model first.")
        // LiteRT-LM runtime initialization is wired in the next phase.
        error("Local AI runtime is not wired yet.")
    }

    override suspend fun createConversation(): AiConversation {
        initializeIfNeeded().getOrThrow()
        return object : AiConversation {
            override suspend fun respond(prompt: String): String = generationMutex.withLock {
                error("Local AI runtime is not wired yet.")
            }

            override fun close() = Unit
        }
    }
}
