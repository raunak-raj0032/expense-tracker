package com.expensetracker.app.ai

import com.expensetracker.app.core.prefs.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoutingAiManager @Inject constructor(
    private val userPreferences: UserPreferences,
    private val localSlmAiManager: LocalSlmAiManager,
    private val ollamaAiManager: OllamaAiManager
) : OnDeviceAiManager {

    override fun observeAvailability(): Flow<AiAvailability> =
        userPreferences.aiModelState
            .map { it.backend }
            .flatMapLatest { backend -> delegate(backend).observeAvailability() }

    override suspend fun downloadModel(): Result<Unit> = delegate().downloadModel()

    override suspend fun deleteModel() = delegate().deleteModel()

    override suspend fun initializeIfNeeded(): Result<Unit> = delegate().initializeIfNeeded()

    override suspend fun createConversation(): AiConversation = delegate().createConversation()

    private suspend fun delegate(): OnDeviceAiManager = delegate(userPreferences.aiModelState.first().backend)

    private fun delegate(backend: AiBackend): OnDeviceAiManager = when (backend) {
        AiBackend.LOCAL -> localSlmAiManager
        AiBackend.OLLAMA -> ollamaAiManager
    }
}
