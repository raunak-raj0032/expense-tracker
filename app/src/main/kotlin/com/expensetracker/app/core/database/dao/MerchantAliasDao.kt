package com.expensetracker.app.core.database.dao

import androidx.room.*
import com.expensetracker.app.core.database.entity.MerchantAliasEntity

@Dao
interface MerchantAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alias: MerchantAliasEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(aliases: List<MerchantAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(aliases: List<MerchantAliasEntity>)

    @Query("SELECT * FROM merchant_aliases WHERE merchantId = :merchantId")
    suspend fun getByMerchantId(merchantId: Long): List<MerchantAliasEntity>

    @Query("SELECT * FROM merchant_aliases ORDER BY id ASC")
    suspend fun getAllForBackup(): List<MerchantAliasEntity>
}
