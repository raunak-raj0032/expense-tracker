package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MerchantEntity::class,
            parentColumns = ["id"],
            childColumns = ["merchantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["merchantId"]),
        Index(value = ["transactionTime"]),
        Index(value = ["fingerprintHash"]),
        Index(value = ["status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String = "INR",
    val transactionTime: Long,
    val accountId: Long,
    val counterpartyAccountId: Long? = null,
    val categoryId: Long? = null,
    val merchantId: Long? = null,
    val description: String? = null,
    val notes: String? = null,
    val paymentMethod: String? = null,
    val source: String = "MANUAL",
    val status: String = "CONFIRMED",
    val externalRef: String? = null,
    val fingerprintHash: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)