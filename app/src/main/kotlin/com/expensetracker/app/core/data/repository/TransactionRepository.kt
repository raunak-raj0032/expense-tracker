package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.dao.TransactionTagDao
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.database.dao.MerchantDao
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.database.entity.TransactionTagEntity
import com.expensetracker.app.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionTagDao: TransactionTagDao,
    private val categoryDao: CategoryDao,
    private val merchantDao: MerchantDao
) {
    private fun LocalDate.startOfDayMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun LocalDate.endExclusiveMillis(): Long =
        plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun observeAll(): Flow<List<Transaction>> = transactionDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    fun observeByAccount(accountId: Long): Flow<List<Transaction>> =
        transactionDao.observeByAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeSuggested(): Flow<List<Transaction>> =
        transactionDao.observeSuggested().map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeForDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> {
        val startTime = start.startOfDayMillis()
        val endTime = end.endExclusiveMillis()
        return transactionDao.observeForDateRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getForDateRange(start: LocalDate, end: LocalDate): List<Transaction> {
        val startTime = start.startOfDayMillis()
        val endTime = end.endExclusiveMillis()
        return transactionDao.getForDateRange(startTime, endTime).map { it.toDomain() }
    }

    suspend fun insert(transaction: Transaction): Long {
        val entity = transaction.toEntity()
        val id = transactionDao.insert(entity)
        transaction.tags.forEach { tagId ->
            transactionTagDao.insert(TransactionTagEntity(id, tagId))
        }
        return id
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
        transactionTagDao.deleteAllForTransaction(transaction.id)
        transaction.tags.forEach { tagId ->
            transactionTagDao.insert(TransactionTagEntity(transaction.id, tagId))
        }
    }

    suspend fun delete(id: Long) = transactionDao.softDelete(id)

    suspend fun getById(id: Long): Transaction? = transactionDao.getById(id)?.toDomain()

    suspend fun findByFingerprint(hash: String): Transaction? =
        transactionDao.findByFingerprint(hash)?.toDomain()

    suspend fun getExpenseTotal(start: LocalDate, end: LocalDate): Long {
        val startTime = start.startOfDayMillis()
        val endTime = end.endExclusiveMillis()
        return transactionDao.getExpenseTotalForRange(startTime, endTime)
    }

    suspend fun getIncomeTotal(start: LocalDate, end: LocalDate): Long {
        val startTime = start.startOfDayMillis()
        val endTime = end.endExclusiveMillis()
        return transactionDao.getIncomeTotalForRange(startTime, endTime)
    }

    suspend fun getCategoryBreakdown(start: LocalDate, end: LocalDate): List<CategoryBreakdown> {
        val startTime = start.startOfDayMillis()
        val endTime = end.endExclusiveMillis()
        return transactionDao.getCategoryBreakdown(startTime, endTime).map { row ->
            val category = row.categoryId?.let { categoryDao.getById(it) }
            CategoryBreakdown(
                categoryId = row.categoryId ?: 0,
                categoryName = category?.name ?: "Unknown",
                total = row.total,
                percentage = 0f
            )
        }
    }

    suspend fun getMerchantBreakdown(start: LocalDate, end: LocalDate, limit: Int = 10): List<MerchantBreakdown> {
        val startTime = start.startOfDayMillis()
        val endTime = end.endExclusiveMillis()
        return transactionDao.getMerchantBreakdown(startTime, endTime, limit).map { row ->
            val merchant = row.merchantId?.let { merchantDao.getById(it) }
            MerchantBreakdown(
                merchantId = row.merchantId ?: 0,
                merchantName = merchant?.canonicalName ?: "Unknown",
                total = row.total,
                transactionCount = row.count
            )
        }
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            type = TransactionType.valueOf(type),
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            transactionTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(transactionTime), ZoneId.systemDefault()),
            accountId = accountId,
            counterpartyAccountId = counterpartyAccountId,
            categoryId = categoryId,
            merchantId = merchantId,
            description = description,
            notes = notes,
            paymentMethod = paymentMethod,
            source = CaptureSourceType.valueOf(source),
            status = TransactionStatus.valueOf(status),
            externalRef = externalRef,
            fingerprintHash = fingerprintHash,
            createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
            updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault()),
            deletedAt = deletedAt?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) },
            tags = emptyList()
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            type = type.name,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            transactionTime = transactionTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            accountId = accountId,
            counterpartyAccountId = counterpartyAccountId,
            categoryId = categoryId,
            merchantId = merchantId,
            description = description,
            notes = notes,
            paymentMethod = paymentMethod,
            source = source.name,
            status = status.name,
            externalRef = externalRef,
            fingerprintHash = fingerprintHash,
            createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt = updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            deletedAt = deletedAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
    }
}
