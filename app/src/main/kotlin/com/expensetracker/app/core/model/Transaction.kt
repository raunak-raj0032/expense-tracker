package com.expensetracker.app.core.model

import java.time.LocalDateTime

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amountMinor: Long,
    val currencyCode: String = "INR",
    val transactionTime: LocalDateTime,
    val accountId: Long,
    val counterpartyAccountId: Long? = null,
    val categoryId: Long? = null,
    val merchantId: Long? = null,
    val description: String? = null,
    val notes: String? = null,
    val paymentMethod: String? = null,
    val source: CaptureSourceType = CaptureSourceType.MANUAL,
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    val externalRef: String? = null,
    val fingerprintHash: String? = null,
    val tags: List<Long> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null
)
