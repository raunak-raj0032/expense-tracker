package com.expensetracker.app.core.database

import com.expensetracker.app.core.database.entity.AccountEntity
import com.expensetracker.app.core.database.entity.TagEntity

object SeedData {
    val defaultAccounts = listOf(
        AccountEntity(name = "Cash", type = "CASH"),
        AccountEntity(name = "Bank Account", type = "BANK"),
        AccountEntity(name = "UPI Wallet", type = "WALLET")
    )

    val defaultTags = listOf(
        TagEntity(name = "Food", colorHex = "#FF5722"),
        TagEntity(name = "Transport", colorHex = "#2196F3"),
        TagEntity(name = "Shopping", colorHex = "#9C27B0"),
        TagEntity(name = "Bills", colorHex = "#F44336"),
        TagEntity(name = "Entertainment", colorHex = "#E91E63"),
        TagEntity(name = "Health", colorHex = "#4CAF50"),
        TagEntity(name = "Rent", colorHex = "#795548"),
        TagEntity(name = "Trip", colorHex = "#03A9F4"),
        TagEntity(name = "Office", colorHex = "#3F51B5"),
        TagEntity(name = "Reimbursable", colorHex = "#009688"),
        TagEntity(name = "Family", colorHex = "#E91E63")
    )

    suspend fun seed(db: ExpenseDatabase) {
        defaultAccounts.forEach { db.accountDao().insert(it) }
        defaultTags.forEach { db.tagDao().insert(it) }
    }
}
