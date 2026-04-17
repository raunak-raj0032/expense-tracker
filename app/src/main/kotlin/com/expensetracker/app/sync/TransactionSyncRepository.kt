package com.expensetracker.app.sync

import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.core.model.CaptureSourceType
import com.expensetracker.app.core.model.TransactionStatus
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.prefs.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val transactionRepository: TransactionRepository,
    private val userPreferences: UserPreferences
) {
    private fun userCollection() = firebaseAuth.currentUser?.uid?.let {
        firestore.collection("users").document(it).collection("transactions")
    }

    suspend fun pushAll(): SyncResult {
        val col = userCollection() ?: return SyncResult.NotSignedIn
        return runCatching {
            val txns = transactionRepository.getForDateRange(LocalDate.of(1970, 1, 1), LocalDate.now().plusDays(1))
            val batch = firestore.batch()
            txns.forEach { t ->
                val doc = col.document(t.id.toString())
                batch.set(doc, t.toFirestoreMap(), SetOptions.merge())
            }
            batch.commit().await()
            userPreferences.setLastSyncMillis(System.currentTimeMillis())
            SyncResult.Success(txns.size)
        }.getOrElse { SyncResult.Failure(it.message ?: "push failed") }
    }

    suspend fun pullAll(): SyncResult {
        val col = userCollection() ?: return SyncResult.NotSignedIn
        return runCatching {
            val snapshot = col.get().await()
            var applied = 0
            snapshot.documents.forEach { doc ->
                val t = doc.toTransactionOrNull() ?: return@forEach
                val existing = transactionRepository.getById(t.id)
                if (existing == null) {
                    transactionRepository.insert(t.copy(id = 0))
                } else if (t.updatedAt.isAfter(existing.updatedAt)) {
                    transactionRepository.update(t)
                }
                applied++
            }
            userPreferences.setLastSyncMillis(System.currentTimeMillis())
            SyncResult.Success(applied)
        }.getOrElse { SyncResult.Failure(it.message ?: "pull failed") }
    }

    suspend fun lastSyncMillis(): Long = userPreferences.lastSyncMillis.first()

    private fun Transaction.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type.name,
        "amountMinor" to amountMinor,
        "currencyCode" to currencyCode,
        "transactionTime" to transactionTime.toMillis(),
        "accountId" to accountId,
        "categoryId" to categoryId,
        "merchantId" to merchantId,
        "description" to description,
        "notes" to notes,
        "paymentMethod" to paymentMethod,
        "source" to source.name,
        "status" to status.name,
        "externalRef" to externalRef,
        "fingerprintHash" to fingerprintHash,
        "createdAt" to createdAt.toMillis(),
        "updatedAt" to updatedAt.toMillis(),
        "deletedAt" to deletedAt?.toMillis()
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toTransactionOrNull(): Transaction? {
        val id = getLong("id") ?: return null
        val typeStr = getString("type") ?: return null
        val amount = getLong("amountMinor") ?: return null
        val accountId = getLong("accountId") ?: return null
        val timeMillis = getLong("transactionTime") ?: return null
        return Transaction(
            id = id,
            type = TransactionType.valueOf(typeStr),
            amountMinor = amount,
            currencyCode = getString("currencyCode") ?: "INR",
            transactionTime = timeMillis.toLocalDateTime(),
            accountId = accountId,
            categoryId = getLong("categoryId"),
            merchantId = getLong("merchantId"),
            description = getString("description"),
            notes = getString("notes"),
            paymentMethod = getString("paymentMethod"),
            source = getString("source")?.let { CaptureSourceType.valueOf(it) } ?: CaptureSourceType.MANUAL,
            status = getString("status")?.let { TransactionStatus.valueOf(it) } ?: TransactionStatus.CONFIRMED,
            externalRef = getString("externalRef"),
            fingerprintHash = getString("fingerprintHash"),
            createdAt = (getLong("createdAt") ?: timeMillis).toLocalDateTime(),
            updatedAt = (getLong("updatedAt") ?: timeMillis).toLocalDateTime(),
            deletedAt = getLong("deletedAt")?.toLocalDateTime()
        )
    }

    private fun LocalDateTime.toMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun Long.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), ZoneId.systemDefault())
}

sealed interface SyncResult {
    data class Success(val count: Int) : SyncResult
    data class Failure(val message: String) : SyncResult
    data object NotSignedIn : SyncResult
}
