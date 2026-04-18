package com.expensetracker.app.core.database.dao

import androidx.room.*
import com.expensetracker.app.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY transactionTime DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY transactionTime DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND accountId = :accountId ORDER BY transactionTime DESC")
    fun observeByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE deletedAt IS NULL 
        AND transactionTime >= :startTime 
        AND transactionTime < :endTime 
        ORDER BY transactionTime DESC
    """)
    fun observeForDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE deletedAt IS NULL 
        AND transactionTime >= :startTime 
        AND transactionTime < :endTime 
        ORDER BY transactionTime DESC
    """)
    suspend fun getForDateRange(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("""
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions 
        WHERE deletedAt IS NULL 
        AND type = 'EXPENSE' 
        AND transactionTime >= :startTime 
        AND transactionTime < :endTime
    """)
    suspend fun getExpenseTotalForRange(startTime: Long, endTime: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions 
        WHERE deletedAt IS NULL 
        AND type = 'INCOME' 
        AND transactionTime >= :startTime 
        AND transactionTime < :endTime
    """)
    suspend fun getIncomeTotalForRange(startTime: Long, endTime: Long): Long

    @Query("""
        SELECT categoryId, COALESCE(SUM(amountMinor), 0) as total 
        FROM transactions 
        WHERE deletedAt IS NULL 
        AND type = 'EXPENSE' 
        AND transactionTime >= :startTime 
        AND transactionTime < :endTime
        GROUP BY categoryId
    """)
    suspend fun getCategoryBreakdown(startTime: Long, endTime: Long): List<CategoryTotal>

    @Query("""
        SELECT merchantId, COALESCE(SUM(amountMinor), 0) as total, COUNT(*) as count
        FROM transactions 
        WHERE deletedAt IS NULL 
        AND type = 'EXPENSE' 
        AND transactionTime >= :startTime 
        AND transactionTime < :endTime
        GROUP BY merchantId
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getMerchantBreakdown(startTime: Long, endTime: Long, limit: Int = 10): List<MerchantTotal>

    @Query("SELECT * FROM transactions WHERE fingerprintHash = :hash AND deletedAt IS NULL LIMIT 1")
    suspend fun findByFingerprint(hash: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = 'SUGGESTED' AND deletedAt IS NULL ORDER BY transactionTime DESC")
    fun observeSuggested(): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE deletedAt IS NOT NULL")
    suspend fun purgeDeleted()
}

data class CategoryTotal(
    val categoryId: Long?,
    val total: Long
)

data class MerchantTotal(
    val merchantId: Long?,
    val total: Long,
    val count: Int
)