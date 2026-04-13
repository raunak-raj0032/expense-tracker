package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val kind: String,
    val parentId: Long? = null,
    val iconKey: String = "category",
    val colorHex: String = "#4CAF50",
    val sortOrder: Int = 0,
    val isSystem: Boolean = false,
    val isArchived: Boolean = false
)