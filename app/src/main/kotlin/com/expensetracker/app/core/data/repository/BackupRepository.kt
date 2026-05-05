package com.expensetracker.app.core.data.repository

import androidx.room.withTransaction
import com.expensetracker.app.auth.AuthRepository
import com.expensetracker.app.auth.AuthState
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.entity.AccountEntity
import com.expensetracker.app.core.database.entity.BudgetEntity
import com.expensetracker.app.core.database.entity.CaptureEventEntity
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.database.entity.MerchantAliasEntity
import com.expensetracker.app.core.database.entity.MerchantEntity
import com.expensetracker.app.core.database.entity.RuleEntity
import com.expensetracker.app.core.database.entity.TagEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.database.entity.TransactionTagEntity
import com.expensetracker.app.core.device.DeviceIdProvider
import com.expensetracker.app.core.prefs.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

enum class RestoreMode { MERGE, REWRITE }

data class BackupPreview(
    val version: Int,
    val createdAt: Long,
    val deviceId: String,
    val backupEmail: String,
    val isSameDevice: Boolean,
    val counts: Map<String, Int>
)

@Singleton
class BackupRepository @Inject constructor(
    private val db: ExpenseDatabase,
    private val prefs: UserPreferences,
    private val authRepository: AuthRepository,
    private val deviceIdProvider: DeviceIdProvider
) {
    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val email = prefs.backupEmail.first().trim()
        require(email.isNotEmpty()) { "Set a backup email before exporting." }

        JSONObject()
            .put("version", BACKUP_VERSION)
            .put("deviceId", deviceIdProvider.getDeviceId())
            .put("createdAt", System.currentTimeMillis())
            .put("backupEmail", email)
            .put("preferences", JSONObject(prefs.backupSnapshot()))
            .put("accounts", db.accountDao().getAllForBackup().toJsonArray(::accountToJson))
            .put("categories", db.categoryDao().getAllForBackup().toJsonArray(::categoryToJson))
            .put("tags", db.tagDao().getAllForBackup().toJsonArray(::tagToJson))
            .put("merchants", db.merchantDao().getAllForBackup().toJsonArray(::merchantToJson))
            .put("merchantAliases", db.merchantAliasDao().getAllForBackup().toJsonArray(::merchantAliasToJson))
            .put("transactions", db.transactionDao().getAllForBackup().toJsonArray(::transactionToJson))
            .put("transactionTags", db.transactionTagDao().getAllForBackup().toJsonArray(::transactionTagToJson))
            .put("captureEvents", db.captureEventDao().getAllForBackup().toJsonArray(::captureEventToJson))
            .put("rules", db.ruleDao().getAllForBackup().toJsonArray(::ruleToJson))
            .put("budgets", db.budgetDao().getAllForBackup().toJsonArray(::budgetToJson))
            .toString(2)
    }

    suspend fun previewBackup(rawJson: String): BackupPreview = withContext(Dispatchers.IO) {
        val root = JSONObject(rawJson)
        val version = root.optInt("version")
        require(version == BACKUP_VERSION) { "Unsupported backup version: $version" }
        val deviceId = root.optString("deviceId")
        val backupEmail = root.optString("backupEmail")
        requireCurrentUserEmailMatches(backupEmail)
        BackupPreview(
            version = version,
            createdAt = root.optLong("createdAt"),
            deviceId = deviceId,
            backupEmail = backupEmail,
            isSameDevice = deviceId == deviceIdProvider.getDeviceId(),
            counts = mapOf(
                "Accounts" to root.optJSONArray("accounts").lengthOrZero(),
                "Categories" to root.optJSONArray("categories").lengthOrZero(),
                "Tags" to root.optJSONArray("tags").lengthOrZero(),
                "Merchants" to root.optJSONArray("merchants").lengthOrZero(),
                "Transactions" to root.optJSONArray("transactions").lengthOrZero(),
                "Capture events" to root.optJSONArray("captureEvents").lengthOrZero(),
                "Rules" to root.optJSONArray("rules").lengthOrZero(),
                "Budgets" to root.optJSONArray("budgets").lengthOrZero()
            )
        )
    }

    suspend fun generateVerificationCode(rawJson: String): String = withContext(Dispatchers.IO) {
        val preview = previewBackup(rawJson)
        val configuredEmail = prefs.backupEmail.first().trim()
        require(configuredEmail.equals(preview.backupEmail, ignoreCase = true)) {
            "This backup belongs to ${preview.backupEmail}. Set that email here before restoring on a different device."
        }
        SecureRandom().nextInt(1_000_000).toString().padStart(6, '0')
    }

    suspend fun restoreBackup(rawJson: String, mode: RestoreMode) = withContext(Dispatchers.IO) {
        val root = JSONObject(rawJson)
        val version = root.optInt("version")
        require(version == BACKUP_VERSION) { "Unsupported backup version: $version" }
        requireCurrentUserEmailMatches(root.optString("backupEmail"))
        val preferences = root.optJSONObject("preferences")?.toMap().orEmpty()

        val accounts = root.optJSONArray("accounts").toList(::jsonToAccount)
        val categories = root.optJSONArray("categories").toList(::jsonToCategory)
        val tags = root.optJSONArray("tags").toList(::jsonToTag)
        val merchants = root.optJSONArray("merchants").toList(::jsonToMerchant)
        val merchantAliases = root.optJSONArray("merchantAliases").toList(::jsonToMerchantAlias)
        val transactions = root.optJSONArray("transactions").toList(::jsonToTransaction)
        val transactionTags = root.optJSONArray("transactionTags").toList(::jsonToTransactionTag)
        val captureEvents = root.optJSONArray("captureEvents").toList(::jsonToCaptureEvent)
        val rules = root.optJSONArray("rules").toList(::jsonToRule)
        val budgets = root.optJSONArray("budgets").toList(::jsonToBudget)

        if (mode == RestoreMode.REWRITE) {
            db.clearAllTables()
        }
        db.withTransaction {
            if (mode == RestoreMode.REWRITE) {
                db.accountDao().insertAllReplace(accounts)
                db.categoryDao().insertAllReplace(categories)
                db.tagDao().insertAllReplace(tags)
                db.merchantDao().insertAllReplace(merchants)
                db.merchantAliasDao().insertAllReplace(merchantAliases)
                db.transactionDao().insertAllReplace(transactions)
                db.transactionTagDao().insertAllReplace(transactionTags)
                db.captureEventDao().insertAllReplace(captureEvents)
                db.ruleDao().insertAllReplace(rules)
                db.budgetDao().insertAllReplace(budgets)
            } else {
                db.accountDao().insertAllIgnore(accounts)
                db.categoryDao().insertAllIgnore(categories)
                db.tagDao().insertAllIgnore(tags)
                db.merchantDao().insertAllIgnore(merchants)
                db.merchantAliasDao().insertAllIgnore(merchantAliases)
                db.transactionDao().insertAllIgnore(transactions)
                db.transactionTagDao().insertAllIgnore(transactionTags)
                db.captureEventDao().insertAllIgnore(captureEvents)
                db.ruleDao().insertAllIgnore(rules)
                db.budgetDao().insertAllIgnore(budgets)
            }
        }
        prefs.restoreBackupSnapshot(preferences)
    }

    private fun accountToJson(item: AccountEntity) = JSONObject()
        .put("id", item.id).put("name", item.name).put("type", item.type)
        .put("currencyCode", item.currencyCode).put("openingBalance", item.openingBalance)
        .put("isArchived", item.isArchived).put("createdAt", item.createdAt).put("updatedAt", item.updatedAt)

    private fun jsonToAccount(json: JSONObject) = AccountEntity(
        id = json.getLong("id"), name = json.getString("name"), type = json.getString("type"),
        currencyCode = json.optString("currencyCode", "INR"), openingBalance = json.optLong("openingBalance"),
        isArchived = json.optBoolean("isArchived"), createdAt = json.optLong("createdAt"), updatedAt = json.optLong("updatedAt")
    )

    private fun categoryToJson(item: CategoryEntity) = JSONObject()
        .put("id", item.id).put("name", item.name).put("kind", item.kind).putNullable("parentId", item.parentId)
        .put("iconKey", item.iconKey).put("colorHex", item.colorHex).put("sortOrder", item.sortOrder)
        .put("isSystem", item.isSystem).put("isArchived", item.isArchived)

    private fun jsonToCategory(json: JSONObject) = CategoryEntity(
        id = json.getLong("id"), name = json.getString("name"), kind = json.getString("kind"),
        parentId = json.optLongOrNull("parentId"), iconKey = json.optString("iconKey", "category"),
        colorHex = json.optString("colorHex", "#4CAF50"), sortOrder = json.optInt("sortOrder"),
        isSystem = json.optBoolean("isSystem"), isArchived = json.optBoolean("isArchived")
    )

    private fun tagToJson(item: TagEntity) = JSONObject()
        .put("id", item.id).put("name", item.name).put("colorHex", item.colorHex).put("createdAt", item.createdAt)

    private fun jsonToTag(json: JSONObject) = TagEntity(
        id = json.getLong("id"), name = json.getString("name"), colorHex = json.optString("colorHex", "#2196F3"), createdAt = json.optLong("createdAt")
    )

    private fun merchantToJson(item: MerchantEntity) = JSONObject()
        .put("id", item.id).put("canonicalName", item.canonicalName).put("normalizedKey", item.normalizedKey)
        .putNullable("categoryHintId", item.categoryHintId).put("createdAt", item.createdAt).put("updatedAt", item.updatedAt)

    private fun jsonToMerchant(json: JSONObject) = MerchantEntity(
        id = json.getLong("id"), canonicalName = json.getString("canonicalName"), normalizedKey = json.getString("normalizedKey"),
        categoryHintId = json.optLongOrNull("categoryHintId"), createdAt = json.optLong("createdAt"), updatedAt = json.optLong("updatedAt")
    )

    private fun merchantAliasToJson(item: MerchantAliasEntity) = JSONObject()
        .put("id", item.id).put("merchantId", item.merchantId).put("aliasText", item.aliasText).put("normalizedKey", item.normalizedKey)

    private fun jsonToMerchantAlias(json: JSONObject) = MerchantAliasEntity(
        id = json.getLong("id"), merchantId = json.getLong("merchantId"), aliasText = json.getString("aliasText"), normalizedKey = json.getString("normalizedKey")
    )

    private fun transactionToJson(item: TransactionEntity) = JSONObject()
        .put("id", item.id).put("type", item.type).put("amountMinor", item.amountMinor).put("currencyCode", item.currencyCode)
        .put("transactionTime", item.transactionTime).put("accountId", item.accountId).putNullable("counterpartyAccountId", item.counterpartyAccountId)
        .putNullable("categoryId", item.categoryId).putNullable("merchantId", item.merchantId).putNullable("description", item.description)
        .putNullable("notes", item.notes).putNullable("paymentMethod", item.paymentMethod).put("source", item.source).put("status", item.status)
        .putNullable("externalRef", item.externalRef).putNullable("fingerprintHash", item.fingerprintHash)
        .put("createdAt", item.createdAt).put("updatedAt", item.updatedAt).putNullable("deletedAt", item.deletedAt)

    private fun jsonToTransaction(json: JSONObject) = TransactionEntity(
        id = json.getLong("id"), type = json.getString("type"), amountMinor = json.getLong("amountMinor"), currencyCode = json.optString("currencyCode", "INR"),
        transactionTime = json.getLong("transactionTime"), accountId = json.getLong("accountId"), counterpartyAccountId = json.optLongOrNull("counterpartyAccountId"),
        categoryId = json.optLongOrNull("categoryId"), merchantId = json.optLongOrNull("merchantId"), description = json.optStringOrNull("description"),
        notes = json.optStringOrNull("notes"), paymentMethod = json.optStringOrNull("paymentMethod"), source = json.optString("source", "MANUAL"),
        status = json.optString("status", "CONFIRMED"), externalRef = json.optStringOrNull("externalRef"), fingerprintHash = json.optStringOrNull("fingerprintHash"),
        createdAt = json.optLong("createdAt"), updatedAt = json.optLong("updatedAt"), deletedAt = json.optLongOrNull("deletedAt")
    )

    private fun transactionTagToJson(item: TransactionTagEntity) = JSONObject().put("transactionId", item.transactionId).put("tagId", item.tagId)

    private fun jsonToTransactionTag(json: JSONObject) = TransactionTagEntity(json.getLong("transactionId"), json.getLong("tagId"))

    private fun captureEventToJson(item: CaptureEventEntity) = JSONObject()
        .put("id", item.id).putNullable("sourceAppPackage", item.sourceAppPackage).put("sourceType", item.sourceType).put("receivedAt", item.receivedAt)
        .putNullable("rawTitle", item.rawTitle).putNullable("rawText", item.rawText).putNullable("rawSubtext", item.rawSubtext)
        .putNullable("parsedAmountMinor", item.parsedAmountMinor).putNullable("parsedDirection", item.parsedDirection).putNullable("parsedMerchant", item.parsedMerchant)
        .putNullable("parsedReference", item.parsedReference).put("confidenceScore", item.confidenceScore.toDouble()).put("parseStatus", item.parseStatus)
        .putNullable("fingerprintHash", item.fingerprintHash).putNullable("linkedTransactionId", item.linkedTransactionId)

    private fun jsonToCaptureEvent(json: JSONObject) = CaptureEventEntity(
        id = json.getLong("id"), sourceAppPackage = json.optStringOrNull("sourceAppPackage"), sourceType = json.getString("sourceType"), receivedAt = json.optLong("receivedAt"),
        rawTitle = json.optStringOrNull("rawTitle"), rawText = json.optStringOrNull("rawText"), rawSubtext = json.optStringOrNull("rawSubtext"),
        parsedAmountMinor = json.optLongOrNull("parsedAmountMinor"), parsedDirection = json.optStringOrNull("parsedDirection"), parsedMerchant = json.optStringOrNull("parsedMerchant"),
        parsedReference = json.optStringOrNull("parsedReference"), confidenceScore = json.optDouble("confidenceScore", 0.0).toFloat(), parseStatus = json.optString("parseStatus", "PENDING"),
        fingerprintHash = json.optStringOrNull("fingerprintHash"), linkedTransactionId = json.optLongOrNull("linkedTransactionId")
    )

    private fun ruleToJson(item: RuleEntity) = JSONObject()
        .put("id", item.id).put("name", item.name).put("priority", item.priority).put("isEnabled", item.isEnabled)
        .put("matchJson", item.matchJson).put("actionJson", item.actionJson).put("createdAt", item.createdAt).put("updatedAt", item.updatedAt)

    private fun jsonToRule(json: JSONObject) = RuleEntity(
        id = json.getLong("id"), name = json.getString("name"), priority = json.optInt("priority"), isEnabled = json.optBoolean("isEnabled", true),
        matchJson = json.getString("matchJson"), actionJson = json.getString("actionJson"), createdAt = json.optLong("createdAt"), updatedAt = json.optLong("updatedAt")
    )

    private fun budgetToJson(item: BudgetEntity) = JSONObject()
        .put("id", item.id).put("name", item.name).put("periodType", item.periodType).put("amountMinor", item.amountMinor).put("currencyCode", item.currencyCode)
        .putNullable("categoryId", item.categoryId).putNullable("accountId", item.accountId).putNullable("tagId", item.tagId)
        .put("startDate", item.startDate).putNullable("endDate", item.endDate).put("isEnabled", item.isEnabled)

    private fun jsonToBudget(json: JSONObject) = BudgetEntity(
        id = json.getLong("id"), name = json.getString("name"), periodType = json.getString("periodType"), amountMinor = json.getLong("amountMinor"),
        currencyCode = json.optString("currencyCode", "INR"), categoryId = json.optLongOrNull("categoryId"), accountId = json.optLongOrNull("accountId"),
        tagId = json.optLongOrNull("tagId"), startDate = json.getLong("startDate"), endDate = json.optLongOrNull("endDate"), isEnabled = json.optBoolean("isEnabled", true)
    )

    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray = JSONArray().also { array ->
        forEach { array.put(mapper(it)) }
    }

    private fun <T> JSONArray?.toList(mapper: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return List(length()) { index -> mapper(getJSONObject(index)) }
    }

    private fun JSONObject.putNullable(name: String, value: Any?): JSONObject = put(name, value ?: JSONObject.NULL)

    private fun JSONObject.optLongOrNull(name: String): Long? = if (isNull(name)) null else optLong(name)

    private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else optString(name)

    private fun JSONArray?.lengthOrZero(): Int = this?.length() ?: 0

    private fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { key ->
        when (val value = get(key)) {
            JSONObject.NULL -> null
            else -> value
        }
    }

    private suspend fun requireCurrentUserEmailMatches(backupEmail: String) {
        require(backupEmail.isNotBlank()) { "Backup file is missing its backup email." }
        val state = authRepository.authState.first()
        val userEmail = (state as? AuthState.SignedIn)?.user?.email?.trim().orEmpty()
        require(userEmail.isNotBlank()) { "Sign in with the backup email before importing this backup." }
        require(userEmail.equals(backupEmail.trim(), ignoreCase = true)) {
            "This backup belongs to $backupEmail. Sign in as $backupEmail to import it."
        }
    }

    private companion object {
        const val BACKUP_VERSION = 1
    }
}
