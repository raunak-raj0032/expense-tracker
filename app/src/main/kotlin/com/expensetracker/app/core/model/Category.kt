package com.expensetracker.app.core.model

data class Category(
    val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val parentId: Long? = null,
    val iconKey: String = "category",
    val colorHex: String = "#4CAF50",
    val sortOrder: Int = 0,
    val isSystem: Boolean = false,
    val isArchived: Boolean = false
)

enum class CategoryKind {
    EXPENSE,
    INCOME,
    BOTH
}