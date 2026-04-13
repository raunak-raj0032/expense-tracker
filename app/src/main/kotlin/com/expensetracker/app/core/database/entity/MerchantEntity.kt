package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val canonicalName: String,
    val normalizedKey: String,
    val categoryHintId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)