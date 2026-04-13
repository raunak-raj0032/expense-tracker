package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "capture_events",
    indices = [
        Index(value = ["fingerprintHash"]),
        Index(value = ["linkedTransactionId"]),
        Index(value = ["parseStatus"])
    ]
)
data class CaptureEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceAppPackage: String? = null,
    val sourceType: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val rawTitle: String? = null,
    val rawText: String? = null,
    val rawSubtext: String? = null,
    val parsedAmountMinor: Long? = null,
    val parsedDirection: String? = null,
    val parsedMerchant: String? = null,
    val parsedReference: String? = null,
    val confidenceScore: Float = 0f,
    val parseStatus: String = "PENDING",
    val fingerprintHash: String? = null,
    val linkedTransactionId: Long? = null
)