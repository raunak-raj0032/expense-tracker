package com.expensetracker.app.core.database.dao

import androidx.room.*
import com.expensetracker.app.core.database.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("SELECT * FROM rules WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getAllEnabledOrdered(): List<RuleEntity>

    @Query("SELECT * FROM rules ORDER BY priority DESC")
    fun observeAll(): Flow<List<RuleEntity>>
}
