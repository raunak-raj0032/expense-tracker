package com.expensetracker.app.core.model

import java.time.LocalDateTime

data class Merchant(
    val id: Long = 0,
    val canonicalName: String,
    val normalizedKey: String,
    val categoryHintId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)