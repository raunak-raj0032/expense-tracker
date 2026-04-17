package com.expensetracker.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.SeedData
import com.expensetracker.app.core.database.dao.*
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
        SeedData.seed(db)
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
