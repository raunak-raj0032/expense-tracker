package com.expensetracker.app.core.database.dao

import androidx.room.*
import com.expensetracker.app.core.database.entity.MerchantAliasEntity

@Dao
interface MerchantAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alias: MerchantAliasEntity): Long

    @Query("SELECT * FROM merchant_aliases WHERE merchantId = :merchantId")
    suspend fun getByMerchantId(merchantId: Long): List<MerchantAliasEntity>
}