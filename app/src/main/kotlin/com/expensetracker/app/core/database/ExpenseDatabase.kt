package com.expensetracker.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expensetracker.app.core.database.dao.*
import com.expensetracker.app.core.database.entity.*

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        MerchantEntity::class,
        MerchantAliasEntity::class,
        TransactionEntity::class,
        TransactionTagEntity::class,
        CaptureEventEntity::class,
        RuleEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun merchantDao(): MerchantDao
    abstract fun merchantAliasDao(): MerchantAliasDao
    abstract fun captureEventDao(): CaptureEventDao
    abstract fun ruleDao(): RuleDao
    abstract fun transactionTagDao(): TransactionTagDao
}