package com.expensetracker.app.statement

import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.database.dao.MerchantDao
import com.expensetracker.app.core.database.entity.MerchantEntity
import com.expensetracker.app.core.model.CaptureSourceType
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.core.model.TransactionStatus
import com.expensetracker.app.core.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class StatementPreviewEntry(
    val transactionTime: LocalDateTime,
    val description: String,
    val amountMinor: Long,
    val direction: TransactionType,
    val paymentMethod: String?,
    val categoryHint: String?,
    val merchantName: String?,
    val reference: String?,
    val rawLine: String,
    val fingerprint: String,
    val isDuplicate: Boolean
)

data class StatementImportPreview(
    val sourceName: String,
    val entries: List<StatementPreviewEntry>,
    val ignoredLineCount: Int,
    val expenseTotal: Long,
    val incomeTotal: Long,
    val duplicateCount: Int
)

data class StatementImportResult(
    val importedCount: Int,
    val duplicateCount: Int
)

@Singleton
class StatementImportRepository @Inject constructor(
    private val parser: StatementImportParser,
    private val transactionRepository: TransactionRepository,
    private val categoryDao: CategoryDao,
    private val merchantDao: MerchantDao
) {

    suspend fun previewDocument(
        documentName: String,
        mimeType: String?,
        bytes: ByteArray,
        password: String? = null
    ): StatementImportPreview = withContext(Dispatchers.Default) {
        toPreview(
            parser.parseDocument(
                documentName = documentName,
                mimeType = mimeType,
                bytes = bytes,
                password = password
            )
        )
    }

    suspend fun previewPastedText(
        sourceName: String,
        rawText: String
    ): StatementImportPreview = withContext(Dispatchers.Default) {
        toPreview(
            parser.parseText(
                sourceName = sourceName,
                rawText = rawText
            )
        )
    }

    suspend fun importPreview(
        preview: StatementImportPreview,
        accountId: Long
    ): StatementImportResult {
        var importedCount = 0
        var duplicateCount = 0

        preview.entries.forEach { entry ->
            val alreadyExists = entry.isDuplicate || transactionRepository.findByFingerprint(entry.fingerprint) != null
            if (alreadyExists) {
                duplicateCount += 1
                return@forEach
            }

            val categoryId = resolveCategoryId(entry.direction, entry.categoryHint)
            val merchantId = resolveMerchantId(entry.merchantName, categoryId)

            transactionRepository.insert(
                Transaction(
                    type = entry.direction,
                    amountMinor = entry.amountMinor,
                    transactionTime = entry.transactionTime,
                    accountId = accountId,
                    categoryId = categoryId,
                    merchantId = merchantId,
                    description = entry.description,
                    notes = "Imported from statement: ${preview.sourceName}\n${entry.rawLine}",
                    paymentMethod = entry.paymentMethod,
                    source = CaptureSourceType.STATEMENT_IMPORT,
                    status = TransactionStatus.CONFIRMED,
                    externalRef = entry.reference,
                    fingerprintHash = entry.fingerprint
                )
            )
            importedCount += 1
        }

        return StatementImportResult(
            importedCount = importedCount,
            duplicateCount = duplicateCount
        )
    }

    private suspend fun toPreview(parsed: ParsedStatementSource): StatementImportPreview {
        val entries = parsed.entries.map { draft ->
            StatementPreviewEntry(
                transactionTime = draft.transactionTime,
                description = draft.description,
                amountMinor = draft.amountMinor,
                direction = draft.direction,
                paymentMethod = draft.paymentMethod,
                categoryHint = draft.categoryHint,
                merchantName = draft.merchantName,
                reference = draft.reference,
                rawLine = draft.rawLine,
                fingerprint = draft.fingerprint,
                isDuplicate = transactionRepository.findByFingerprint(draft.fingerprint) != null
            )
        }

        return StatementImportPreview(
            sourceName = parsed.sourceName,
            entries = entries,
            ignoredLineCount = parsed.ignoredLineCount,
            expenseTotal = entries.filter { it.direction == TransactionType.EXPENSE }.sumOf { it.amountMinor },
            incomeTotal = entries.filter { it.direction == TransactionType.INCOME }.sumOf { it.amountMinor },
            duplicateCount = entries.count { it.isDuplicate }
        )
    }

    private suspend fun resolveCategoryId(direction: TransactionType, categoryHint: String?): Long? {
        val hint = when (direction) {
            TransactionType.EXPENSE,
            TransactionType.INCOME -> categoryHint
            TransactionType.TRANSFER,
            TransactionType.REFUND -> "Transfer"
        } ?: return null

        return categoryDao.findActiveByName(hint)?.id
    }

    private suspend fun resolveMerchantId(merchantName: String?, categoryId: Long?): Long? {
        if (merchantName.isNullOrBlank()) {
            return null
        }

        val normalizedKey = merchantName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "")
        merchantDao.findByNormalizedKey(normalizedKey)?.let { return it.id }
        merchantDao.findByAlias(normalizedKey)?.let { return it.id }

        val insertedId = merchantDao.insert(
            MerchantEntity(
                canonicalName = merchantName,
                normalizedKey = normalizedKey,
                categoryHintId = categoryId
            )
        )

        return if (insertedId != -1L) {
            insertedId
        } else {
            merchantDao.findByNormalizedKey(normalizedKey)?.id
        }
    }
}
