package com.expensetracker.app.core.database.dao

import androidx.room.*
import com.expensetracker.app.core.database.entity.MerchantEntity

@Dao
interface MerchantDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(merchant: MerchantEntity): Long

    @Update
    suspend fun update(merchant: MerchantEntity)

    @Query("SELECT * FROM merchants WHERE normalizedKey = :key LIMIT 1")
    suspend fun findByNormalizedKey(key: String): MerchantEntity?

    @Query("SELECT * FROM merchants ORDER BY canonicalName ASC LIMIT :limit")
    suspend fun getTopMerchants(limit: Int = 20): List<MerchantEntity>

    @Query("SELECT * FROM merchants WHERE id = :id")
    suspend fun getById(id: Long): MerchantEntity?

    @Query("""
        SELECT m.* FROM merchants m
        INNER JOIN merchant_aliases a ON m.id = a.merchantId
        WHERE a.normalizedKey = :key
        LIMIT 1
    """)
    suspend fun findByAlias(key: String): MerchantEntity?
}
