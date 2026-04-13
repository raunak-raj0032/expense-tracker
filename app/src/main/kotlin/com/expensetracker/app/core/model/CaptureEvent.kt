package com.expensetracker.app.core.model

import java.time.LocalDateTime

data class CaptureEvent(
    val id: Long = 0,
    val sourceAppPackage: String?,
    val sourceType: CaptureSourceType,
    val receivedAt: LocalDateTime,
    val rawTitle: String?,
    val rawText: String?,
    val rawSubtext: String?,
    val parsedAmountMinor: Long? = null,
    val parsedDirection: TransactionType? = null,
    val parsedMerchant: String? = null,
    val parsedReference: String? = null,
    val confidenceScore: Float = 0f,
    val parseStatus: ParseStatus = ParseStatus.PENDING,
    val fingerprintHash: String? = null,
    val linkedTransactionId: Long? = null
)

enum class ParseStatus {
    PENDING,
    SUCCESS,
    IGNORED,
    FAILED
}