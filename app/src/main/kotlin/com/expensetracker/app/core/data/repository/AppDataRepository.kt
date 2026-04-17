package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.SeedData
import com.expensetracker.app.core.prefs.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataRepository @Inject constructor(
    private val database: ExpenseDatabase,
    private val userPreferences: UserPreferences
) {
    suspend fun deleteAllEntries() {
        database.clearAllTables()
        SeedData.seed(database)
        userPreferences.clearSyncState()
    }
}
