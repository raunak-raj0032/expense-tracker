package com.expensetracker.app.ai

sealed class AiAvailability {
    data class Unsupported(val reasons: List<AiCompatibility.Reason>) : AiAvailability()
    object NeedsDownload : AiAvailability()
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : AiAvailability()
    object Ready : AiAvailability()
    object Initializing : AiAvailability()
    data class Error(val message: String) : AiAvailability()
}
