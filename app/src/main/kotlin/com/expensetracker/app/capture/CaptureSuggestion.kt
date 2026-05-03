package com.expensetracker.app.capture

import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.model.CaptureSourceType
import java.time.LocalDateTime

data class CaptureSuggestion(
    val id: Long,
    val description: String,
    val amountMinor: Long,
    val direction: TransactionType,
    val merchant: String?,
    val paymentMethod: String?,
    val categoryHint: String?,
    val reference: String?,
    val sourceType: CaptureSourceType,
    val sourceLabel: String,
    val rawPreview: String,
    val confidence: Float,
    val receivedAt: LocalDateTime,
    val isPeerTransfer: Boolean
)

data class AccessibilityCaptureOutcome(
    val eventId: Long?,
    val autoImported: Boolean
)
