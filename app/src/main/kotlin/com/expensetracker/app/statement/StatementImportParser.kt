package com.expensetracker.app.statement

import com.expensetracker.app.capture.PaymentMessageParser
import com.expensetracker.app.core.model.TransactionType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class StatementImportDraft(
    val transactionTime: LocalDateTime,
    val description: String,
    val amountMinor: Long,
    val direction: TransactionType,
    val paymentMethod: String?,
    val categoryHint: String?,
    val merchantName: String?,
    val reference: String?,
    val rawLine: String,
    val fingerprint: String
)

class StatementParseException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

data class ParsedStatementSource(
    val sourceName: String,
    val entries: List<StatementImportDraft>,
    val ignoredLineCount: Int
)

@Singleton
class StatementImportParser @Inject constructor(
    private val paymentMessageParser: PaymentMessageParser
) {

    fun parseDocument(
        documentName: String,
        mimeType: String?,
        bytes: ByteArray
    ): ParsedStatementSource {
        val isPdf = mimeType == "application/pdf" || documentName.lowercase(Locale.ROOT).endsWith(".pdf")
        
        val text = when {
            isPdf -> {
                extractPdfText(bytes).getOrElse { ex ->
                    throw StatementParseException(
                        message = "Unable to read PDF file. The document may be password-protected, corrupted, or an image-based (scanned) PDF that cannot be parsed. " +
                            "Try converting it to text or CSV format, or copy-paste the statement text instead.",
                        cause = ex
                    )
                }
            }
            else -> String(bytes, Charsets.UTF_8)
        }
        
        if (text.isBlank() && isPdf) {
            throw StatementParseException(
                message = "The PDF file '$documentName' appears to be empty or contains no extractable text. " +
                    "Image-based PDFs (scanned documents) cannot be parsed. Please use a text-based PDF, " +
                    "export to CSV, or paste the statement text directly."
            )
        }
        
        return parseText(
            sourceName = documentName,
            rawText = text,
            preferDelimited = mimeType == "text/csv" ||
                documentName.lowercase(Locale.ROOT).endsWith(".csv") ||
                documentName.lowercase(Locale.ROOT).endsWith(".tsv")
        )
    }

    fun parseText(
        sourceName: String,
        rawText: String,
        preferDelimited: Boolean = false
    ): ParsedStatementSource {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return ParsedStatementSource(sourceName = sourceName, entries = emptyList(), ignoredLineCount = 0)
        }

        if (preferDelimited) {
            val delimited = parseDelimitedText(sourceName, trimmed)
            if (delimited.entries.isNotEmpty()) {
                return delimited
            }
        }

        val fallbackDelimited = parseDelimitedText(sourceName, trimmed)
        if (fallbackDelimited.entries.isNotEmpty()) {
            return fallbackDelimited
        }

        val freeform = parseFreeformText(sourceName, trimmed)
        if (freeform.entries.isNotEmpty()) {
            return freeform
        }

        return ParsedStatementSource(
            sourceName = sourceName,
            entries = emptyList(),
            ignoredLineCount = trimmed.lines().size
        )
    }

    private fun extractPdfText(bytes: ByteArray): Result<String> {
        return runCatching {
            PDDocument.load(bytes).use { document ->
                if (document.isEncrypted) {
                    throw StatementParseException("The PDF is password-protected. Please remove the password and try again.")
                }
                val text = PDFTextStripper().getText(document)
                if (text.isBlank()) {
                    throw StatementParseException("PDF contains no extractable text. It may be a scanned/image-based PDF.")
                }
                text
            }
        }
    }

    private fun parseDelimitedText(sourceName: String, text: String): ParsedStatementSource {
        val allLines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        
        if (allLines.size < 2) {
            return ParsedStatementSource(sourceName, emptyList(), ignoredLineCount = allLines.size)
        }

        val delimiter = detectDelimiter(allLines.first())
        val header = splitDelimitedLine(allLines.first(), delimiter)
        val headerNormalized = header.map(::normalizeHeader)
        
        val hasDateColumn = headerNormalized.any { it.contains("date") }
        val hasAmountColumn = headerNormalized.any { 
            it.contains("amount") || it == "debit" || it == "credit" || 
            it == "dr" || it == "cr" || it == "withdrawal" || it == "deposit"
        }
        
        if (!hasDateColumn || !hasAmountColumn) {
            return ParsedStatementSource(sourceName, emptyList(), ignoredLineCount = 0)
        }

        var ignoredLineCount = 0
        val entries = mutableListOf<StatementImportDraft>()
        
        for (i in 1 until allLines.size) {
            val line = allLines[i]
            val fields = splitDelimitedLine(line, delimiter)
            
            if (fields.size < 2) {
                ignoredLineCount++
                continue
            }
            
            val draft = parseDelimitedRowFlexible(
                headerNormalized = headerNormalized,
                headerOriginal = header,
                fields = fields,
                sourceName = sourceName
            )
            
            if (draft != null) {
                entries.add(draft)
            } else {
                ignoredLineCount++
            }
        }

        return ParsedStatementSource(
            sourceName = sourceName,
            entries = entries,
            ignoredLineCount = ignoredLineCount
        )
    }

    private fun parseDelimitedRowFlexible(
        headerNormalized: List<String>,
        headerOriginal: List<String>,
        fields: List<String>,
        sourceName: String
    ): StatementImportDraft? {
        val row = buildMap {
            headerNormalized.forEachIndexed { index, key ->
                put(key, fields.getOrNull(index)?.trim() ?: "")
            }
        }

        val dateFieldIndex = headerNormalized.indexOfFirst { it.contains("date") || it.contains("txndate") || it.contains("valuedate") }
        val dateText = if (dateFieldIndex >= 0) fields.getOrNull(dateFieldIndex) else null
        
        val date = dateText?.let { findAndParseDate(it) } ?: return null

        val amountFields = listOf("amount", "transactionamount", "txnamount", "debit", "withdrawal", "dr", "credit", "deposit", "cr", "amountdue", "payment")
        val descFields = listOf("description", "details", "narration", "particulars", "merchant", "remarks", "transactiondescription", "transactiondetails")
        val refFields = listOf("reference", "ref", "transactionid", "utr", "rrn", "txnid", "orderid")

        var description = ""
        for (field in descFields) {
            row[field]?.takeIf { it.isNotBlank() }?.let { 
                if (description.isBlank()) description = it 
                else description += " " + it
            }
        }
        for (field in refFields) {
            row[field]?.takeIf { it.isNotBlank() }?.let { 
                description += " " + it
            }
        }
        description = description.trim().normalizeWhitespace()
        
        if (description.isBlank()) {
            description = fields.joinToString(" ").take(100)
        }

        val typeValue = row.entries.find { it.key == "type" || it.key == "drcr" || it.key == "transactiontype" }?.value?.trim()?.lowercase()
        
        val debitValue = row.entries.find { it.key in listOf("debit", "withdrawal", "dr") }?.value?.takeIf { it.isNotBlank() }
        val creditValue = row.entries.find { it.key in listOf("credit", "deposit", "cr") }?.value?.takeIf { it.isNotBlank() }
        val amountValue = row.entries.find { it.key in listOf("amount", "transactionamount", "txnamount", "amountdue", "payment") }?.value?.takeIf { it.isNotBlank() }

        val amountAndDirection = when {
            debitValue != null -> {
                parseAmountMinor(debitValue)?.let { it to TransactionType.EXPENSE }
            }
            creditValue != null -> {
                parseAmountMinor(creditValue)?.let { it to TransactionType.INCOME }
            }
            amountValue != null -> {
                val parsed = parseAmountMinor(amountValue)
                if (parsed != null) {
                    val isNegative = amountValue.contains("-") || amountValue.startsWith("(")
                    val hasCr = amountValue.contains("cr", ignoreCase = true)
                    val hasDr = amountValue.contains("dr", ignoreCase = true)
                    
                    val typeBasedDirection = when {
                        typeValue?.contains("cr") == true || typeValue?.contains("credit") == true -> TransactionType.INCOME
                        typeValue?.contains("dr") == true || typeValue?.contains("debit") == true -> TransactionType.EXPENSE
                        else -> null
                    }
                    
                    when {
                        typeBasedDirection != null -> parsed to typeBasedDirection
                        hasCr -> parsed to TransactionType.INCOME
                        hasDr -> parsed to TransactionType.EXPENSE
                        isNegative -> parsed to TransactionType.INCOME
                        else -> parsed to TransactionType.EXPENSE
                    }
                } else null
            }
            else -> null
        } ?: return null

        return buildDraft(
            sourceName = sourceName,
            date = date,
            description = description,
            amountMinor = amountAndDirection.first,
            direction = amountAndDirection.second,
            rawLine = fields.joinToString(" | ")
        )
    }

    private fun parseFreeformText(sourceName: String, text: String): ParsedStatementSource {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { skipLineKeywords.any { kw -> it.contains(kw, ignoreCase = true) } }
            .toList()

        var ignoredLineCount = 0
        val entries = mutableListOf<StatementImportDraft>()

        for (line in lines) {
            val draft = parseFreeformLine(sourceName, line)
            if (draft != null) {
                entries.add(draft)
            } else {
                ignoredLineCount++
            }
        }

        return ParsedStatementSource(
            sourceName = sourceName,
            entries = entries,
            ignoredLineCount = ignoredLineCount
        )
    }

    private fun parseFreeformLine(sourceName: String, line: String): StatementImportDraft? {
        val dateMatch = findDateInLine(line) ?: return null
        val date = dateMatch.second ?: return null
        
        val beforeDate = line.substring(0, dateMatch.first.first)
        val afterDate = line.substring(dateMatch.first.last + 1)
        
        val segments = (beforeDate + " " + afterDate)
            .split(Regex("[\\s]{2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (segments.isEmpty()) return null

        var amountMinor: Long? = null
        var direction: TransactionType = TransactionType.EXPENSE
        var amountIndex = -1
        
        for ((i, seg) in segments.withIndex()) {
            val parsed = parseAmountMinor(seg)
            if (parsed != null) {
                amountMinor = parsed
                amountIndex = i
                
                val hasCr = seg.contains("cr", ignoreCase = true)
                val hasDr = seg.contains("dr", ignoreCase = true)
                val isNegative = seg.startsWith("-") || seg.startsWith("(") || seg.startsWith("−")
                
                direction = when {
                    hasCr -> TransactionType.INCOME
                    hasDr -> TransactionType.EXPENSE
                    isNegative -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }
                break
            }
        }

        if (amountMinor == null) {
            val amountsInLine = amountRegex.findAll(line).toList()
            if (amountsInLine.isNotEmpty()) {
                val lastAmount = amountsInLine.last()
                val amtStr = lastAmount.value
                amountMinor = parseAmountMinor(amtStr)
                if (amountMinor != null) {
                    val hasCr = amtStr.contains("cr", ignoreCase = true)
                    val hasDr = amtStr.contains("dr", ignoreCase = true)
                    direction = when {
                        hasCr -> TransactionType.INCOME
                        hasDr -> TransactionType.EXPENSE
                        else -> TransactionType.EXPENSE
                    }
                }
            }
        }

        if (amountMinor == null) return null

        val description = segments.filterIndexed { i, _ -> i != amountIndex }
            .joinToString(" ")
            .normalizeWhitespace()
            .takeIf { it.isNotBlank() }
            ?: line.replace(Regex("[0-9.,\\s]+"), "").take(50)

        return buildDraft(
            sourceName = sourceName,
            date = date,
            description = description,
            amountMinor = amountMinor,
            direction = direction,
            rawLine = line
        )
    }

    private fun findDateInLine(line: String): Pair<IntRange, LocalDate?>? {
        for (pattern in datePatterns) {
            val match = pattern.toRegex().find(line)
            if (match != null) {
                val dateStr = match.value
                val date = parseDate(dateStr)
                if (date != null) {
                    return match.range to date
                }
            }
        }
        return null
    }

    private fun findAndParseDate(text: String): LocalDate? {
        val cleanText = text.trim()
        
        for (pattern in datePatterns) {
            val match = pattern.toRegex().find(cleanText)
            if (match != null) {
                val date = parseDate(match.value)
                if (date != null) return date
            }
        }
        
        return parseDate(cleanText)
    }

    private fun buildDraft(
        sourceName: String,
        date: LocalDate,
        description: String,
        amountMinor: Long,
        direction: TransactionType,
        rawLine: String
    ): StatementImportDraft {
        val insight = paymentMessageParser.inferNarrative(
            text = description,
            direction = direction
        )
        val transactionTime = date.atTime(12, 0)
        return StatementImportDraft(
            transactionTime = transactionTime,
            description = description,
            amountMinor = amountMinor,
            direction = direction,
            paymentMethod = insight.paymentMethod,
            categoryHint = insight.categoryHint,
            merchantName = insight.merchant,
            reference = insight.reference,
            rawLine = rawLine,
            fingerprint = buildFingerprint(
                sourceName = sourceName,
                transactionTime = transactionTime,
                description = description,
                amountMinor = amountMinor,
                direction = direction,
                reference = insight.reference
            )
        )
    }

    private fun buildFingerprint(
        sourceName: String,
        transactionTime: LocalDateTime,
        description: String,
        amountMinor: Long,
        direction: TransactionType,
        reference: String?
    ): String {
        return listOf(
            normalizeHeader(sourceName),
            transactionTime.toLocalDate().toString(),
            amountMinor.toString(),
            direction.name,
            normalizeHeader(description),
            normalizeHeader(reference.orEmpty())
        )
            .joinToString("|")
            .hashCode()
            .toString()
    }

    private fun parseAmountMinor(value: String?): Long? {
        if (value.isNullOrBlank()) return null

        val cleaned = value
            .replace(",", "")
            .replace("₹", "", ignoreCase = false)
            .replace("INR", "", ignoreCase = true)
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("Rs.", "", ignoreCase = true)
            .replace("CR", "", ignoreCase = true)
            .replace("DR", "", ignoreCase = true)
            .replace("(", "-")
            .replace(")", "")
            .replace("−", "-")
            .replace("–", "-")
            .replace(" ", "")
            .trim()

        if (cleaned.isBlank() || cleaned == "-") return null

        return cleaned.toBigDecimalOrNull()
            ?.setScale(2, RoundingMode.HALF_UP)
            ?.abs()
            ?.movePointRight(2)
            ?.longValueExact()
    }

    private fun parseDate(value: String): LocalDate? {
        val sanitized = value.trim()
            .replace(Regex("\\s{2,}"), " ")
            .replace("−", "-")
            .replace("–", "-")

        for (formatter in dateFormatters) {
            try {
                return LocalDate.parse(sanitized, formatter)
            } catch (_: DateTimeParseException) {}
        }

        for (formatter in shortDateFormatters) {
            try {
                return LocalDate.parse(sanitized, formatter).withYear(Year.now().value)
            } catch (_: DateTimeParseException) {}
        }

        return null
    }

    private fun detectDelimiter(line: String): Char {
        val counts = mapOf(
            '\t' to line.count { it == '\t' },
            ',' to line.count { it == ',' },
            ';' to line.count { it == ';' },
            '|' to line.count { it == '|' }
        )
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ','
    }

    private fun splitDelimitedLine(line: String, delimiter: Char): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        line.forEach { char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        values += current.toString()
        return values.map { it.trim().trim('"').trim() }
    }

    private fun normalizeHeader(value: String): String {
        return value.lowercase(Locale.ROOT)
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .replace(Regex("[^a-z0-9]"), "")
    }

    private fun String.normalizeWhitespace(): String = trim().replace(Regex("\\s+"), " ")

    private companion object {
        val amountRegex = Regex(
            "(?i)[(\\[\\-]?\\s*(?:₹|rs\\.?|inr)?\\s*[0-9][0-9,]*(?:\\.\\d{1,2})?\\s*(?:cr|dr)?\\s*[)\\]]?"
        )

        val datePatterns = listOf(
            "\\d{1,2}/\\d{1,2}/\\d{2,4}",
            "\\d{1,2}-\\d{1,2}-\\d{2,4}",
            "\\d{4}/\\d{1,2}/\\d{1,2}",
            "\\d{4}-\\d{1,2}-\\d{1,2}",
            "\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{2,4}",
            "[A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{4}",
            "\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}"
        )

        val dateFormatters = listOf(
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH)
        )

        val shortDateFormatters = listOf(
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd/MM").toFormatter(Locale.ENGLISH),
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MM").toFormatter(Locale.ENGLISH),
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM").toFormatter(Locale.ENGLISH)
        )

        val skipLineKeywords = listOf(
            "opening balance", "closing balance", "available balance",
            "available credit", "credit limit", "statement summary",
            "total amount due", "minimum amount due", "reward points",
            "payment due date", "previous balance", "current balance",
            "page ", "statement date", "account number", "customer name",
            "card number", "statement period", "total payment",
            "total purchase", "interest charge", "annual percentage",
            "transaction date", "narration", "card ending"
        )
    }
}
