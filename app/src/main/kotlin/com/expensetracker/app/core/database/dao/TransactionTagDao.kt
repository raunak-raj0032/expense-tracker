package com.expensetracker.app.core.database.dao

import androidx.room.*
import com.expensetracker.app.core.database.entity.TransactionTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(join: TransactionTagEntity)

    @Delete
    suspend fun delete(join: TransactionTagEntity)

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    fun observeTagIdsForTransaction(transactionId: Long): Flow<List<Long>>

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun getTagIdsForTransaction(transactionId: Long): List<Long>

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteAllForTransaction(transactionId: Long)
}