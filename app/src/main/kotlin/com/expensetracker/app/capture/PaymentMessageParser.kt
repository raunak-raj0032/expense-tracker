package com.expensetracker.app.capture

import com.expensetracker.app.core.model.TransactionType
import java.math.RoundingMode
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ParseResult(
    val isTransaction: Boolean = false,
    val amountMinor: Long? = null,
    val direction: TransactionType? = null,
    val merchant: String? = null,
    val reference: String? = null,
    val confidence: Float = 0f,
    val fingerprint: String? = null,
    val paymentMethod: String? = null,
    val categoryHint: String? = null,
    val description: String? = null,
    val isPeerTransfer: Boolean = false
)

data class NarrativeInsight(
    val merchant: String? = null,
    val reference: String? = null,
    val paymentMethod: String? = null,
    val categoryHint: String? = null,
    val isPeerTransfer: Boolean = false
)

private data class MerchantHint(
    val canonicalName: String,
    val category: String
)

@Singleton
class PaymentMessageParser @Inject constructor() {

    fun parse(
        packageName: String? = null,
        sender: String? = null,
        title: String = "",
        text: String = "",
        subtext: String = ""
    ): ParseResult {
        val combined = listOf(sender, title, text, subtext)
            .filterNotNull()
            .joinToString(" ")
            .normalizeWhitespace()

        if (combined.isBlank()) {
            return ParseResult()
        }

        val amountMinor = extractAmountMinor(combined)
        val direction = detectDirection(combined)

        if (amountMinor == null || direction == null) {
            return ParseResult()
        }

        val insight = inferNarrative(
            text = combined,
            packageName = packageName,
            sender = sender,
            direction = direction
        )

        val description = buildDescription(
            direction = direction,
            merchant = insight.merchant,
            paymentMethod = insight.paymentMethod,
            isPeerTransfer = insight.isPeerTransfer
        )

        val confidence = calculateConfidence(
            merchant = insight.merchant,
            reference = insight.reference,
            paymentMethod = insight.paymentMethod,
            categoryHint = insight.categoryHint,
            isPeerTransfer = insight.isPeerTransfer
        )

        val fingerprint = buildFingerprint(
            amountMinor = amountMinor,
            direction = direction,
            merchant = insight.merchant,
            reference = insight.reference,
            paymentMethod = insight.paymentMethod,
            sender = sender,
            packageName = packageName
        )

        return ParseResult(
            isTransaction = true,
            amountMinor = amountMinor,
            direction = direction,
            merchant = insight.merchant,
            reference = insight.reference,
            confidence = confidence,
            fingerprint = fingerprint,
            paymentMethod = insight.paymentMethod,
            categoryHint = insight.categoryHint,
            description = description,
            isPeerTransfer = insight.isPeerTransfer
        )
    }

    fun inferNarrative(
        text: String,
        packageName: String? = null,
        sender: String? = null,
        direction: TransactionType? = null
    ): NarrativeInsight {
        val combined = listOfNotNull(sender, text)
            .joinToString(" ")
            .normalizeWhitespace()
        if (combined.isBlank()) {
            return NarrativeInsight()
        }

        val paymentMethod = detectPaymentMethod(packageName, combined)
        val reference = extractReference(combined)
        val rawParty = extractParty(combined)
        val rawPartyHint = rawParty?.let(::classifyMerchant)
        // Only scan the full screen text for known merchants when we couldn't
        // extract an explicit party — otherwise ads/promos ("Ola", "power bill")
        // on a GPay/PhonePe screen would override the real recipient.
        val textHint = if (rawParty == null) classifyMerchant(combined) else null
        val fallbackSenderHint = sender
            ?.takeIf(::looksMeaningfulSender)
            ?.let(::classifyMerchant)
        val merchantName = rawPartyHint?.canonicalName
            ?: rawParty
            ?: textHint?.canonicalName
            ?: fallbackSenderHint?.canonicalName
            ?: sender?.takeIf(::looksMeaningfulSender)

        val isPeerTransfer = (paymentMethod == "UPI" || paymentMethod == "Wallet") &&
            merchantName != null &&
            rawPartyHint == null &&
            textHint == null &&
            looksLikePerson(merchantName)

        val categoryHint = when {
            direction == TransactionType.INCOME && combined.contains("salary", ignoreCase = true) -> "Salary"
            direction == TransactionType.TRANSFER || isWalletTopUp(combined.lowercase(Locale.ROOT)) -> "Transfer"
            rawPartyHint != null -> rawPartyHint.category
            textHint != null -> textHint.category
            isPeerTransfer -> "Transfer"
            direction == TransactionType.INCOME && paymentMethod == "UPI" -> "Transfer"
            else -> null
        }

        return NarrativeInsight(
            merchant = merchantName,
            reference = reference,
            paymentMethod = paymentMethod,
            categoryHint = categoryHint,
            isPeerTransfer = isPeerTransfer
        )
    }

    private fun detectDirection(text: String): TransactionType? {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            isWalletTopUp(lower) || transferKeywords.any(lower::contains) -> TransactionType.TRANSFER
            incomeKeywords.any(lower::contains) -> TransactionType.INCOME
            expenseKeywords.any(lower::contains) -> TransactionType.EXPENSE
            else -> null
        }
    }

    private fun detectPaymentMethod(packageName: String?, text: String): String? {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            isWalletTopUp(lower) -> "Wallet"
            packageName != null && upiPackages.contains(packageName) -> "UPI"
            lower.contains("upi") || lower.contains("vpa") -> "UPI"
            lower.contains("debit card") || lower.contains("credit card") || lower.contains(" card ") -> "Card"
            lower.contains("netbanking") || lower.contains("imps") || lower.contains("neft") || lower.contains("rtgs") -> "Bank Transfer"
            lower.contains("wallet") || lower.contains("amazon pay balance") || lower.contains("wallet balance") -> "Wallet"
            packageName != null && bankPackages.contains(packageName) -> "Bank Transfer"
            else -> null
        }
    }

    private fun extractAmountMinor(text: String): Long? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text) ?: continue
            val wholePart = match.groupValues[1].replace(",", "")
            val decimalPart = match.groupValues.getOrNull(2).orEmpty()
            val amount = buildString {
                append(wholePart)
                if (decimalPart.isNotEmpty()) {
                    append('.')
                    append(decimalPart.padEnd(2, '0').take(2))
                }
            }

            return amount.toBigDecimalOrNull()
                ?.setScale(2, RoundingMode.HALF_UP)
                ?.movePointRight(2)
                ?.longValueExact()
        }
        return null
    }

    private fun extractReference(text: String): String? {
        for (pattern in referencePatterns) {
            val match = pattern.find(text) ?: continue
            return match.groupValues[1]
                .trim()
                .trim('.', ',', ':', '-', '#')
        }
        return null
    }

    private fun extractParty(text: String): String? {
        for (pattern in partyPatterns) {
            val match = pattern.find(text) ?: continue
            val candidate = sanitizeParty(match.groupValues[1])
            if (candidate != null) {
                return candidate
            }
        }
        return null
    }

    private fun sanitizeParty(candidate: String): String? {
        var value = candidate
            .replace(Regex("[\\n\\r\\t]"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .trim('.', ',', ';', ':', '-', '(', ')')

        if (value.isBlank()) {
            return null
        }

        val lowered = value.lowercase(Locale.ROOT)
        if (stopPartyTokens.any { lowered == it || lowered.startsWith("$it ") }) {
            return null
        }

        if ("@" in value) {
            value = value.substringBefore('@')
                .replace('.', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .normalizeWhitespace()
        }

        value = value.replace(Regex("^(mr|mrs|ms|dr)\\.?\\s+", RegexOption.IGNORE_CASE), "")
        value = value.replace(
            Regex("\\b(?:via|using|through|on|for|ref|utr|txn|transaction|upi|avl|available|a/c|acct)\\b.*$", RegexOption.IGNORE_CASE),
            ""
        ).normalizeWhitespace()

        if (value.length < 2 || value.any(Char::isDigit) && !value.contains(" ")) {
            return null
        }

        return value.take(40).trim()
    }

    private fun classifyMerchant(value: String): MerchantHint? {
        merchantKeywordToHint[normalizeKey(value)]?.let {
            return MerchantHint(canonicalName = it.canonicalName, category = it.category)
        }

        // Tokenise on non-alphanumeric so keywords like "power" only match a
        // standalone word, not substrings inside "Powered by" or URL slugs.
        val tokens = value.lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }
            .toHashSet()
        val match = merchantKeywordToHint.entries.firstOrNull { (keyword, _) ->
            keyword in tokens
        }?.value
        return match?.let { MerchantHint(canonicalName = it.canonicalName, category = it.category) }
    }

    private fun buildDescription(
        direction: TransactionType,
        merchant: String?,
        paymentMethod: String?,
        isPeerTransfer: Boolean
    ): String {
        val label = merchant ?: paymentMethod ?: "captured payment"
        return when (direction) {
            TransactionType.INCOME -> "Received from $label"
            TransactionType.EXPENSE -> if (isPeerTransfer) "Sent to $label" else "Paid to $label"
            TransactionType.TRANSFER -> "Transfer with $label"
            TransactionType.REFUND -> "Refund from $label"
        }
    }

    private fun calculateConfidence(
        merchant: String?,
        reference: String?,
        paymentMethod: String?,
        categoryHint: String?,
        isPeerTransfer: Boolean
    ): Float {
        var score = 0.45f
        if (merchant != null) score += 0.2f
        if (paymentMethod != null) score += 0.15f
        if (reference != null) score += 0.1f
        if (categoryHint != null) score += 0.1f
        if (isPeerTransfer) score += 0.05f
        return score.coerceAtMost(0.95f)
    }

    private fun buildFingerprint(
        amountMinor: Long,
        direction: TransactionType,
        merchant: String?,
        reference: String?,
        paymentMethod: String?,
        sender: String?,
        packageName: String?
    ): String {
        return listOfNotNull(
            amountMinor.toString(),
            direction.name,
            merchant?.let(::normalizeKey),
            reference?.uppercase(Locale.ROOT),
            paymentMethod,
            sender?.let(::normalizeKey),
            packageName
        )
            .joinToString("|")
            .hashCode()
            .toString()
    }

    private fun looksLikePerson(value: String): Boolean {
        val normalized = normalizeKey(value)
        if (normalized.length < 3) {
            return false
        }
        if (merchantKeywordToHint.keys.any(normalized::contains)) {
            return false
        }
        return value.split(" ").size <= 3 && value.all { it.isLetter() || it == ' ' }
    }

    private fun looksMeaningfulSender(value: String): Boolean {
        val normalized = normalizeKey(value)
        return normalized.length >= 3 && normalized !in genericSenderKeys
    }

    private fun isWalletTopUp(lower: String): Boolean {
        return walletTopUpKeywords.any(lower::contains) &&
            (lower.contains("wallet") || lower.contains("balance") || lower.contains("amazon pay"))
    }

    private fun normalizeKey(value: String): String {
        return value.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "")
    }

    private fun String.normalizeWhitespace(): String =
        trim().replace(Regex("\\s+"), " ")

    private companion object {
        val amountPatterns = listOf(
            Regex("(?i)(?:\\u20B9|rs\\.?|inr)\\s*([0-9][0-9,]*)(?:\\.(\\d{1,2}))?"),
            Regex("(?i)(?:amount|amt)\\s*(?:debited|credited|paid|spent|sent)?\\s*[:\\-]?\\s*([0-9][0-9,]*)(?:\\.(\\d{1,2}))?"),
            Regex("(?i)([0-9][0-9,]*)(?:\\.(\\d{1,2}))?\\s*(?:rs\\.?|inr)")
        )

        val referencePatterns = listOf(
            Regex("(?i)(?:upi\\s*ref(?:erence)?(?:\\s*no)?|utr|txn(?:\\s*id)?|ref(?:erence)?(?:\\s*no)?|order\\s*id)\\s*[:#\\-]?\\s*([A-Z0-9\\-]{6,})")
        )

        val partyPatterns = listOf(
            Regex("(?i)paid to\\s+([A-Za-z0-9@._&\\- ]{2,50})"),
            Regex("(?i)payment to\\s+([A-Za-z0-9@._&\\- ]{2,50})"),
            Regex("(?i)sent to\\s+([A-Za-z0-9@._&\\- ]{2,50})"),
            // PhonePe accessibility screens: "Banking name: SATYAM SAHIL"
            Regex("(?i)banking name:?\\s+([A-Za-z][A-Za-z0-9 .&\\-']{1,49})"),
            // UPI VPAs: "satyamsahil@oksbi" — use the handle as the party name
            Regex("([A-Za-z][A-Za-z0-9._\\-]{2,40})@[a-z]{2,20}"),
            Regex("(?i)transferred to\\s+([A-Za-z0-9@._&\\- ]{2,50})"),
            Regex("(?i)received from\\s+([A-Za-z0-9@._&\\- ]{2,50})"),
            Regex("(?i)from\\s+([A-Za-z0-9@._&\\- ]{2,50})\\s+(?:via|through|using|on|for)"),
            Regex("(?i)to\\s+([A-Za-z0-9@._&\\- ]{2,50})\\s+(?:via|through|using|on|for|ref|utr|upi)"),
            Regex("(?i)(?:spent|purchase(?:d)?|payment|order)\\s+(?:at|on)\\s+([A-Za-z0-9@._&\\- ]{2,50})"),
            Regex("(?i)(?:at|towards)\\s+([A-Za-z0-9@._&\\- ]{2,60})\\s+(?:on|using|via|ref|utr|upi|txn)"),
            Regex("(?i)merchant\\s*[:\\-]\\s*([A-Za-z0-9@._&\\- ]{2,50})"),
            Regex("(?i)vpa\\s*[:\\-]\\s*([A-Za-z0-9._\\-]+@[A-Za-z0-9._\\-]+)")
        )

        val expenseKeywords = listOf(
            "paid",
            "debited",
            "spent",
            "sent",
            "purchase",
            "purchased",
            "withdrawn",
            "charged",
            "payment made",
            "successful payment",
            "paid via",
            "spent at"
        )

        val incomeKeywords = listOf(
            "received",
            "credited",
            "deposited",
            "refund",
            "reversed",
            "reversal",
            "cashback",
            "settled",
            "paid you"
        )

        val transferKeywords = listOf(
            "wallet top up",
            "wallet top-up",
            "top up",
            "top-up",
            "add money",
            "added money",
            "wallet loaded",
            "added to amazon pay balance",
            "amazon pay balance loaded",
            "load money",
            "balance transfer"
        )

        val walletTopUpKeywords = listOf(
            "wallet top up",
            "wallet top-up",
            "top up",
            "top-up",
            "add money",
            "added money",
            "wallet loaded",
            "balance loaded",
            "load money"
        )

        val upiPackages = setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.google.android.apps.nbu.paisa.provider",
            "com.phonepe.app",
            "com.phonepe.app.preprod",
            "net.one97.paytm",
            "com.paytm.app",
            "in.org.npci.bhimapp",
            "com.dreamplug.androidapp",
            "in.amazon.mShop.android.shopping",
            "com.mobikwik_new",
            "com.freecharge.android",
            "com.whatsapp"
        )

        val bankPackages = setOf(
            "com.axisbank.digibank",
            "com.icici.bank.imobile",
            "com.hdfcbank.mobilebanking",
            "com.sbi.lionmobileservice",
            "com.yesbank",
            "com.csam.icici.bank.imobile",
            "com.kotak.bank.mobile",
            "com.snapwork.hdfc"
        )

        val merchantKeywordToHint = linkedMapOf(
            "amazonpaybalance" to MerchantHint("Amazon Pay Balance", "Transfer"),
            "amazonpayments" to MerchantHint("Amazon Pay", "Transfer"),
            "amazonpay" to MerchantHint("Amazon Pay", "Transfer"),
            "amazon" to MerchantHint("Amazon", "Shopping"),
            "amzn" to MerchantHint("Amazon", "Shopping"),
            "flipkart" to MerchantHint("Flipkart", "Shopping"),
            "myntra" to MerchantHint("Myntra", "Shopping"),
            "ajio" to MerchantHint("Ajio", "Shopping"),
            "meesho" to MerchantHint("Meesho", "Shopping"),
            "swiggy" to MerchantHint("Swiggy", "Food"),
            "zomato" to MerchantHint("Zomato", "Food"),
            "dominos" to MerchantHint("Domino's", "Food"),
            "uber" to MerchantHint("Uber", "Transport"),
            "ola" to MerchantHint("Ola", "Transport"),
            "rapido" to MerchantHint("Rapido", "Transport"),
            "irctc" to MerchantHint("IRCTC", "Transport"),
            "redbus" to MerchantHint("RedBus", "Transport"),
            "airtel" to MerchantHint("Airtel", "Bills"),
            "jio" to MerchantHint("Jio", "Bills"),
            "vodafone" to MerchantHint("Vodafone", "Bills"),
            "bsnl" to MerchantHint("BSNL", "Bills"),
            "electricity" to MerchantHint("Electricity Bill", "Bills"),
            "power" to MerchantHint("Power Bill", "Bills"),
            "water" to MerchantHint("Water Bill", "Bills"),
            "gas" to MerchantHint("Gas Bill", "Bills"),
            "netflix" to MerchantHint("Netflix", "Entertainment"),
            "spotify" to MerchantHint("Spotify", "Entertainment"),
            "bookmyshow" to MerchantHint("BookMyShow", "Entertainment"),
            "hotstar" to MerchantHint("Disney+ Hotstar", "Entertainment"),
            "apollo" to MerchantHint("Apollo", "Health"),
            "pharmeasy" to MerchantHint("PharmEasy", "Health"),
            "mobikwik" to MerchantHint("MobiKwik", "Transfer"),
            "freecharge" to MerchantHint("Freecharge", "Transfer"),
            "payzapp" to MerchantHint("PayZapp", "Transfer"),
            "cred" to MerchantHint("CRED", "Transfer"),
            "whatsapp" to MerchantHint("WhatsApp Pay", "Transfer")
        )

        val stopPartyTokens = setOf(
            "upi",
            "bank",
            "account",
            "a/c",
            "your",
            "wallet",
            "balance",
            "order",
            "payment",
            "transaction"
        )

        val genericSenderKeys = setOf(
            "ad",
            "vm",
            "tm",
            "bk",
            "sms",
            "bank"
        )
    }
}
