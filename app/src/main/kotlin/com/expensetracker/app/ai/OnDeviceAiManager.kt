package com.expensetracker.app.ai

import kotlinx.coroutines.flow.Flow

interface OnDeviceAiManager {
    fun observeAvailability(): Flow<AiAvailability>
    suspend fun downloadModel(): Result<Unit>
    suspend fun deleteModel()
    suspend fun initializeIfNeeded(): Result<Unit>
    suspend fun createConversation(): AiConversation
}

interface AiConversation {
    suspend fun respond(prompt: String): String
    fun close()
}
