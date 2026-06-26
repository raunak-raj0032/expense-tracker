package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.SeedData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AppDataRepository @Inject constructor(
    private val database: ExpenseDatabase
) {
    suspend fun deleteAllEntries() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        SeedData.seed(database)
    }
}
