package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(budgets: List<BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE isEnabled = 1 ORDER BY startDate DESC")
    fun observeEnabled(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isEnabled = 1 AND periodType = 'MONTHLY' ORDER BY startDate DESC LIMIT 1")
    fun observeLatestMonthly(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE isEnabled = 1 AND periodType = :periodType ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestEnabledByPeriod(periodType: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    suspend fun getAllForBackup(): List<BudgetEntity>

    @Query("UPDATE budgets SET isEnabled = 0 WHERE id = :id")
    suspend fun disable(id: Long)
}
