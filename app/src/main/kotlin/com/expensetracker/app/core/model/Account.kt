package com.expensetracker.app.core.model

import java.time.LocalDateTime

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val currencyCode: String = "INR",
    val openingBalance: Long = 0,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)