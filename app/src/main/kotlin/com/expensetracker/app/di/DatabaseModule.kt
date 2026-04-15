package com.expensetracker.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.dao.*
import com.expensetracker.app.core.database.entity.AccountEntity
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.database.entity.TagEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ExpenseDatabase {
        return Room.databaseBuilder(
            context,
            ExpenseDatabase::class.java,
            "expense_tracker.db"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(context)
                    }
                }
            })
            .build()
    }

    private suspend fun seedDatabase(context: Context) {
        val db = Room.databaseBuilder(
            context,
            ExpenseDatabase::class.java,
            "expense_tracker.db"
        ).build()

        val defaultCategories = listOf(
            CategoryEntity(name = "Food", kind = "EXPENSE", iconKey = "food", colorHex = "#FF5722", sortOrder = 1, isSystem = true),
            CategoryEntity(name = "Transport", kind = "EXPENSE", iconKey = "transport", colorHex = "#2196F3", sortOrder = 2, isSystem = true),
            CategoryEntity(name = "Shopping", kind = "EXPENSE", iconKey = "shopping", colorHex = "#9C27B0", sortOrder = 3, isSystem = true),
            CategoryEntity(name = "Bills", kind = "EXPENSE", iconKey = "bills", colorHex = "#F44336", sortOrder = 4, isSystem = true),
            CategoryEntity(name = "Entertainment", kind = "EXPENSE", iconKey = "entertainment", colorHex = "#E91E63", sortOrder = 5, isSystem = true),
            CategoryEntity(name = "Health", kind = "EXPENSE", iconKey = "health", colorHex = "#4CAF50", sortOrder = 6, isSystem = true),
            CategoryEntity(name = "Rent", kind = "EXPENSE", iconKey = "home", colorHex = "#795548", sortOrder = 7, isSystem = true),
            CategoryEntity(name = "Salary", kind = "INCOME", iconKey = "salary", colorHex = "#8BC34A", sortOrder = 100, isSystem = true),
            CategoryEntity(name = "Investment", kind = "INCOME", iconKey = "investment", colorHex = "#00BCD4", sortOrder = 101, isSystem = true),
            CategoryEntity(name = "Gift", kind = "INCOME", iconKey = "gift", colorHex = "#FF9800", sortOrder = 102, isSystem = true),
            CategoryEntity(name = "Transfer", kind = "BOTH", iconKey = "transfer", colorHex = "#607D8B", sortOrder = 200, isSystem = true)
        )

        defaultCategories.forEach { db.categoryDao().insert(it) }

        val defaultAccounts = listOf(
            AccountEntity(name = "Cash", type = "CASH"),
            AccountEntity(name = "Bank Account", type = "BANK"),
            AccountEntity(name = "UPI Wallet", type = "WALLET")
        )

        defaultAccounts.forEach { db.accountDao().insert(it) }

        val defaultTags = listOf(
            TagEntity(name = "Trip", colorHex = "#03A9F4"),
            TagEntity(name = "Office", colorHex = "#3F51B5"),
            TagEntity(name = "Reimbursable", colorHex = "#009688"),
            TagEntity(name = "Family", colorHex = "#E91E63")
        )

        defaultTags.forEach { db.tagDao().insert(it) }

        db.close()
    }

    @Provides
    fun provideTransactionDao(db: ExpenseDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideAccountDao(db: ExpenseDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: ExpenseDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTagDao(db: ExpenseDatabase): TagDao = db.tagDao()

    @Provides
    fun provideMerchantDao(db: ExpenseDatabase): MerchantDao = db.merchantDao()

    @Provides
    fun provideMerchantAliasDao(db: ExpenseDatabase): MerchantAliasDao = db.merchantAliasDao()

    @Provides
    fun provideCaptureEventDao(db: ExpenseDatabase): CaptureEventDao = db.captureEventDao()

    @Provides
    fun provideRuleDao(db: ExpenseDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideTransactionTagDao(db: ExpenseDatabase): TransactionTagDao = db.transactionTagDao()

    @Provides
    fun provideBudgetDao(db: ExpenseDatabase): BudgetDao = db.budgetDao()
}
