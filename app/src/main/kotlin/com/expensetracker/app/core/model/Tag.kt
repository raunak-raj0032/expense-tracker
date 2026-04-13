package com.expensetracker.app.core.model

import java.time.LocalDateTime

data class Tag(
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#2196F3",
    val createdAt: LocalDateTime = LocalDateTime.now()
)