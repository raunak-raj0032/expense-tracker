package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_aliases")
data class MerchantAliasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantId: Long,
    val aliasText: String,
    val normalizedKey: String
)