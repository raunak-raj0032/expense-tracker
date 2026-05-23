package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.core.database.entity.CaptureEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CaptureEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(events: List<CaptureEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(events: List<CaptureEventEntity>)

    @Update
    suspend fun update(event: CaptureEventEntity)

    @Query("SELECT * FROM capture_events WHERE parseStatus = 'PENDING' ORDER BY receivedAt DESC")
    fun observePending(): Flow<List<CaptureEventEntity>>

    @Query("SELECT * FROM capture_events WHERE confidenceScore >= 0.7 AND parseStatus = 'SUCCESS' ORDER BY receivedAt DESC")
    fun observeSuggestions(): Flow<List<CaptureEventEntity>>

    @Query("""
        SELECT * FROM capture_events
        WHERE linkedTransactionId IS NULL
        AND parseStatus IN ('PENDING', 'SUCCESS')
        ORDER BY receivedAt DESC
    """)
    fun observeReviewQueue(): Flow<List<CaptureEventEntity>>

    @Query("""
        SELECT COUNT(*) FROM capture_events
        WHERE linkedTransactionId IS NULL
        AND parseStatus IN ('PENDING', 'SUCCESS')
    """)
    fun observeOpenCount(): Flow<Int>

    @Query("SELECT * FROM capture_events WHERE id = :eventId LIMIT 1")
    suspend fun getById(eventId: Long): CaptureEventEntity?

    @Query("SELECT * FROM capture_events ORDER BY id ASC")
    suspend fun getAllForBackup(): List<CaptureEventEntity>

    @Query("SELECT * FROM capture_events WHERE fingerprintHash = :hash AND receivedAt >= :sinceMillis LIMIT 1")
    suspend fun findDuplicate(hash: String, sinceMillis: Long): CaptureEventEntity?

    @Query("""
        SELECT * FROM capture_events
        WHERE sourceAppPackage = :packageName
        AND parsedAmountMinor = :amountMinor
        AND parsedDirection = :direction
        AND receivedAt >= :sinceMillis
        LIMIT 1
    """)
    suspend fun findRecentMatch(
        packageName: String?,
        amountMinor: Long,
        direction: String,
        sinceMillis: Long
    ): CaptureEventEntity?

    @Query("UPDATE capture_events SET linkedTransactionId = :transactionId, parseStatus = 'SUCCESS' WHERE id = :eventId")
    suspend fun linkTransaction(eventId: Long, transactionId: Long)

    @Query("UPDATE capture_events SET parseStatus = 'IGNORED' WHERE id = :eventId")
    suspend fun markIgnored(eventId: Long)

    @Query("UPDATE capture_events SET parseStatus = 'IGNORED' WHERE parseStatus = 'SUGGESTED'")
    suspend fun markAllIgnored()

    @Query("DELETE FROM capture_events WHERE linkedTransactionId IS NOT NULL AND parseStatus = 'SUCCESS'")
    suspend fun purgeLinked()
}
