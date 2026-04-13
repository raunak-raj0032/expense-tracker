package com.expensetracker.app.capture

import android.content.Context
import android.provider.Telephony
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.dao.AccountDao
import com.expensetracker.app.core.database.dao.CaptureEventDao
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.database.dao.MerchantDao
import com.expensetracker.app.core.database.entity.CaptureEventEntity
import com.expensetracker.app.core.database.entity.MerchantEntity
import com.expensetracker.app.core.model.CaptureSourceType
import com.expensetracker.app.core.model.ParseStatus
import com.expensetracker.app.core.model.Transaction
import com.expensetracker.app.core.model.TransactionStatus
import com.expensetracker.app.core.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureEventRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val captureEventDao: CaptureEventDao,
    private val transactionRepository: TransactionRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val merchantDao: MerchantDao,
    private val parserRegistry: NotificationParserRegistry
) {

    fun observeReviewQueue(): Flow<List<CaptureSuggestion>> {
        return captureEventDao.observeReviewQueue().map { events ->
            events.mapNotNull(::toSuggestion)
        }
    }

    fun observeOpenCount(): Flow<Int> = captureEventDao.observeOpenCount()

    suspend fun captureNotification(
        packageName: String,
        title: String,
        text: String,
        subtext: String,
        receivedAt: Long = System.currentTimeMillis()
    ): Boolean {
        val parseResult = parserRegistry.parse(packageName, title, text, subtext)
        return storeCaptureEvent(
            sourceKey = packageName,
            sourceType = CaptureSourceType.NOTIFICATION,
            title = title,
            text = text,
            subtext = subtext,
            receivedAt = receivedAt,
            parseResult = parseResult
        )
    }

    suspend fun captureSms(
        sender: String?,
        body: String,
        receivedAt: Long = System.currentTimeMillis()
    ): Boolean {
        val parseResult = parserRegistry.parseSms(sender, body)
        return storeCaptureEvent(
            sourceKey = sender,
            sourceType = CaptureSourceType.SMS,
            title = sender,
            text = body,
            subtext = null,
            receivedAt = receivedAt,
            parseResult = parseResult
        )
    }

    suspend fun importRecentSms(limit: Int = 40): Int {
        var importedCount = 0
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

            var processed = 0
            while (processed < limit && cursor.moveToNext()) {
                val sender = cursor.getString(addressIndex)
                val body = cursor.getString(bodyIndex) ?: continue
                val receivedAt = cursor.getLong(dateIndex)
                if (captureSms(sender = sender, body = body, receivedAt = receivedAt)) {
                    importedCount += 1
                }
                processed += 1
            }
        }

        return importedCount
    }

    suspend fun addToLedger(eventId: Long): Long {
        val event = captureEventDao.getById(eventId)
            ?: error("Capture event $eventId not found")

        event.linkedTransactionId?.let { return it }

        val parseResult = parseEvent(event)
        val amountMinor = event.parsedAmountMinor ?: parseResult.amountMinor
            ?: error("Capture event $eventId is missing an amount")
        val direction = event.parsedDirection?.let(TransactionType::valueOf) ?: parseResult.direction
            ?: error("Capture event $eventId is missing a direction")
        val fingerprint = event.fingerprintHash ?: parseResult.fingerprint

        if (fingerprint != null) {
            transactionRepository.findByFingerprint(fingerprint)?.let { existing ->
                captureEventDao.linkTransaction(eventId, existing.id)
                return existing.id
            }
        }

        val categoryId = resolveCategoryId(direction, parseResult.categoryHint)
        val merchantId = resolveMerchantId(parseResult.merchant ?: event.parsedMerchant, categoryId)
        val accountId = resolveAccountId(
            paymentMethod = parseResult.paymentMethod,
            rawText = buildRawText(event)
        )

        val transaction = Transaction(
            type = direction,
            amountMinor = amountMinor,
            transactionTime = event.receivedAt.toLocalDateTime(),
            accountId = accountId,
            categoryId = categoryId,
            merchantId = merchantId,
            description = parseResult.description ?: defaultDescription(direction, parseResult.merchant ?: event.parsedMerchant),
            notes = buildNotes(event),
            paymentMethod = parseResult.paymentMethod,
            source = CaptureSourceType.valueOf(event.sourceType),
            status = TransactionStatus.CONFIRMED,
            externalRef = event.parsedReference ?: parseResult.reference,
            fingerprintHash = fingerprint
        )

        val transactionId = transactionRepository.insert(transaction)
        captureEventDao.linkTransaction(eventId, transactionId)
        return transactionId
    }

    suspend fun ignore(eventId: Long) {
        captureEventDao.markIgnored(eventId)
    }

    private suspend fun storeCaptureEvent(
        sourceKey: String?,
        sourceType: CaptureSourceType,
        title: String?,
        text: String,
        subtext: String?,
        receivedAt: Long,
        parseResult: ParseResult
    ): Boolean {
        if (!parseResult.isTransaction || parseResult.fingerprint == null) {
            return false
        }

        if (captureEventDao.findDuplicate(parseResult.fingerprint) != null) {
            return false
        }

        if (transactionRepository.findByFingerprint(parseResult.fingerprint) != null) {
            return false
        }

        val entity = CaptureEventEntity(
            sourceAppPackage = sourceKey,
            sourceType = sourceType.name,
            receivedAt = receivedAt,
            rawTitle = title,
            rawText = text,
            rawSubtext = subtext,
            parsedAmountMinor = parseResult.amountMinor,
            parsedDirection = parseResult.direction?.name,
            parsedMerchant = parseResult.merchant,
            parsedReference = parseResult.reference,
            confidenceScore = parseResult.confidence,
            parseStatus = if (parseResult.confidence >= 0.8f) {
                ParseStatus.SUCCESS.name
            } else {
                ParseStatus.PENDING.name
            },
            fingerprintHash = parseResult.fingerprint
        )

        captureEventDao.insert(entity)
        return true
    }

    private fun toSuggestion(event: CaptureEventEntity): CaptureSuggestion? {
        val parseResult = parseEvent(event)
        val amountMinor = event.parsedAmountMinor ?: parseResult.amountMinor ?: return null
        val direction = event.parsedDirection?.let(TransactionType::valueOf) ?: parseResult.direction ?: return null

        return CaptureSuggestion(
            id = event.id,
            description = parseResult.description ?: defaultDescription(direction, parseResult.merchant ?: event.parsedMerchant),
            amountMinor = amountMinor,
            direction = direction,
            merchant = parseResult.merchant ?: event.parsedMerchant,
            paymentMethod = parseResult.paymentMethod,
            categoryHint = parseResult.categoryHint,
            reference = event.parsedReference ?: parseResult.reference,
            sourceLabel = sourceLabel(event),
            rawPreview = event.rawText.orEmpty().trim().take(180),
            confidence = event.confidenceScore.takeIf { it > 0f } ?: parseResult.confidence,
            receivedAt = event.receivedAt.toLocalDateTime(),
            isPeerTransfer = parseResult.isPeerTransfer
        )
    }

    private fun parseEvent(event: CaptureEventEntity): ParseResult {
        return if (event.sourceType == CaptureSourceType.SMS.name) {
            parserRegistry.parseSms(
                sender = event.sourceAppPackage,
                body = buildRawText(event)
            )
        } else {
            parserRegistry.parse(
                packageName = event.sourceAppPackage.orEmpty(),
                title = event.rawTitle.orEmpty(),
                text = event.rawText.orEmpty(),
                subtext = event.rawSubtext.orEmpty()
            )
        }
    }

    private fun buildRawText(event: CaptureEventEntity): String {
        return listOfNotNull(event.rawTitle, event.rawText, event.rawSubtext)
            .joinToString(" ")
            .trim()
    }

    private suspend fun resolveAccountId(paymentMethod: String?, rawText: String): Long {
        val activeAccounts = accountDao.getAllActive()
        val lower = rawText.lowercase(Locale.ROOT)

        val preferred = when {
            paymentMethod == "UPI" -> activeAccounts.firstOrNull {
                it.name.contains("upi", ignoreCase = true) || it.type == "WALLET"
            }
            paymentMethod == "Bank Transfer" || lower.contains("a/c") || lower.contains("bank") ->
                activeAccounts.firstOrNull { it.type == "BANK" }
            paymentMethod == "Card" ->
                activeAccounts.firstOrNull { it.type == "BANK" }
            else -> activeAccounts.firstOrNull()
        }

        return preferred?.id ?: error("No active account available")
    }

    private suspend fun resolveCategoryId(direction: TransactionType, categoryHint: String?): Long? {
        if (categoryHint == null) {
            return null
        }

        val normalizedHint = when (direction) {
            TransactionType.EXPENSE,
            TransactionType.INCOME -> categoryHint
            TransactionType.TRANSFER,
            TransactionType.REFUND -> "Transfer"
        }

        return categoryDao.findActiveByName(normalizedHint)?.id
    }

    private suspend fun resolveMerchantId(merchantName: String?, categoryId: Long?): Long? {
        if (merchantName.isNullOrBlank()) {
            return null
        }

        val normalizedKey = normalizeKey(merchantName)
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

    private fun buildNotes(event: CaptureEventEntity): String? {
        val lines = buildList {
            if (!event.parsedReference.isNullOrBlank()) {
                add("Reference: ${event.parsedReference}")
            }
            if (!event.rawText.isNullOrBlank()) {
                add(event.rawText.trim())
            }
        }
        return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun defaultDescription(direction: TransactionType, merchant: String?): String {
        val label = merchant ?: "captured payment"
        return when (direction) {
            TransactionType.EXPENSE -> "Paid to $label"
            TransactionType.INCOME -> "Received from $label"
            TransactionType.TRANSFER -> "Transfer with $label"
            TransactionType.REFUND -> "Refund from $label"
        }
    }

    private fun sourceLabel(event: CaptureEventEntity): String {
        return when (event.sourceType) {
            CaptureSourceType.SMS.name -> "SMS ${event.sourceAppPackage?.let { "from $it" } ?: ""}".trim()
            else -> notificationSourceNames[event.sourceAppPackage] ?: event.sourceAppPackage ?: "Notification"
        }
    }

    private fun normalizeKey(value: String): String {
        return value.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "")
    }

    private fun Long.toLocalDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(this),
            ZoneId.systemDefault()
        )
    }

    private companion object {
        val notificationSourceNames = mapOf(
            "com.google.android.apps.nbu.paisa.provider" to "Google Pay",
            "com.phonepe.app" to "PhonePe",
            "com.paytm.app" to "Paytm",
            "in.org.npci.bhimapp" to "BHIM",
            "com.axisbank.digibank" to "Axis Bank",
            "com.icici.bank.imobile" to "ICICI Bank",
            "com.hdfcbank.mobilebanking" to "HDFC Bank",
            "com.sbi.lionmobileservice" to "SBI",
            "com.yesbank" to "YES BANK"
        )
    }
}
