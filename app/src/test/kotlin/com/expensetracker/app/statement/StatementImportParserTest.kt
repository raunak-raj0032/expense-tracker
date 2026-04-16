package com.expensetracker.app.statement

import com.expensetracker.app.capture.PaymentMessageParser
import com.expensetracker.app.core.model.TransactionType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream

class StatementImportParserTest {

    private val parser = StatementImportParser(PaymentMessageParser())

    @Test
    fun parsesCsvStatementRows() {
        val parsed = parser.parseText(
            sourceName = "april-statement.csv",
            rawText = """
                Date,Description,Debit,Credit,Reference
                13/04/2026,Amazon order via UPI,499.00,,UPI123ABC
                14/04/2026,Salary credit,,50000.00,NEFT445566
            """.trimIndent(),
            preferDelimited = true
        )

        assertEquals(2, parsed.entries.size)
        assertEquals(TransactionType.EXPENSE, parsed.entries[0].direction)
        assertEquals(49_900L, parsed.entries[0].amountMinor)
        assertEquals("Amazon", parsed.entries[0].merchantName)
        assertEquals("Shopping", parsed.entries[0].categoryHint)

        assertEquals(TransactionType.INCOME, parsed.entries[1].direction)
        assertEquals(5_000_000L, parsed.entries[1].amountMinor)
        assertEquals("Salary", parsed.entries[1].categoryHint)
    }

    @Test
    fun parsesStatementStyleTextWithBalanceColumn() {
        val parsed = parser.parseText(
            sourceName = "statement.txt",
            rawText = """
                13/04/2026  AMAZON UPI PURCHASE  499.00  7500.00
                14/04/2026  SALARY CREDIT  50000.00 CR  57500.00
            """.trimIndent()
        )

        assertEquals(2, parsed.entries.size)
        assertTrue(parsed.entries.all { it.fingerprint.isNotBlank() })
        assertEquals(TransactionType.EXPENSE, parsed.entries[0].direction)
        assertEquals(49_900L, parsed.entries[0].amountMinor)
        assertEquals(TransactionType.INCOME, parsed.entries[1].direction)
        assertEquals(5_000_000L, parsed.entries[1].amountMinor)
    }

    @Test
    fun parseDocumentDetectsPdfByExtension() {
        val pdfBytes = "%PDF-1.4 fake pdf content".toByteArray()
        try {
            parser.parseDocument(
                documentName = "SBI_Statement.pdf",
                mimeType = null,
                bytes = pdfBytes
            )
            fail("Expected StatementParseException for invalid PDF")
        } catch (e: StatementParseException) {
            assertTrue(e.message.contains("Unable to read PDF"))
        }
    }

    @Test
    fun parseDocumentDetectsPdfByMimeType() {
        val pdfBytes = "%PDF-1.4 fake pdf content".toByteArray()
        try {
            parser.parseDocument(
                documentName = "statement.txt",
                mimeType = "application/pdf",
                bytes = pdfBytes
            )
            fail("Expected StatementParseException for invalid PDF")
        } catch (e: StatementParseException) {
            assertTrue(e.message.contains("Unable to read PDF"))
        }
    }

    @Test
    fun parseDocumentHandlesTextFiles() {
        val text = """
            Date,Description,Amount
            13/04/2026,Test Transaction,100.00
        """.trimIndent()
        
        val parsed = parser.parseDocument(
            documentName = "statement.csv",
            mimeType = null,
            bytes = text.toByteArray()
        )
        
        assertEquals(1, parsed.entries.size)
    }

    @Test
    fun parseDocumentRequiresPasswordForProtectedPdf() {
        val pdfBytes = createPasswordProtectedPdf(password = "secret123")

        try {
            parser.parseDocument(
                documentName = "locked-statement.pdf",
                mimeType = "application/pdf",
                bytes = pdfBytes
            )
            fail("Expected StatementParseException for missing PDF password")
        } catch (e: StatementParseException) {
            assertTrue(e.message.contains("password-protected"))
        }
    }

    @Test
    fun parseDocumentRejectsWrongPasswordForProtectedPdf() {
        val pdfBytes = createPasswordProtectedPdf(password = "secret123")

        try {
            parser.parseDocument(
                documentName = "locked-statement.pdf",
                mimeType = "application/pdf",
                bytes = pdfBytes,
                password = "wrong-pass"
            )
            fail("Expected StatementParseException for incorrect PDF password")
        } catch (e: StatementParseException) {
            assertTrue(e.message.contains("incorrect"))
        }
    }

    @Test
    fun parseExceptionHasMessageAndCause() {
        val cause = IllegalStateException("PDF error")
        val exception = StatementParseException("Test message", cause)
        
        assertEquals("Test message", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun parsesSemicolonDelimitedStatement() {
        val text = """
            Date;Description;Amount;Type
            13/04/2026;Amazon Purchase;499.00;DR
            14/04/2026;Salary Credit;50000.00;CR
        """.trimIndent()
        
        val parsed = parser.parseText(sourceName = "semicolon.csv", rawText = text, preferDelimited = true)
        
        assertEquals(2, parsed.entries.size)
        assertEquals(TransactionType.EXPENSE, parsed.entries[0].direction)
        assertEquals(TransactionType.INCOME, parsed.entries[1].direction)
    }

    @Test
    fun parsesPipeDelimitedStatement() {
        val text = """
            Date|Description|Amount
            13/04/2026|Amazon UPI|₹499.00
            14/04/2026|Salary|₹50,000.00 CR
        """.trimIndent()
        
        val parsed = parser.parseText(sourceName = "pipe.csv", rawText = text, preferDelimited = true)
        
        assertEquals(2, parsed.entries.size)
        assertEquals(49_900L, parsed.entries[0].amountMinor)
        assertEquals(5_000_000L, parsed.entries[1].amountMinor)
    }

    @Test
    fun parsesTabDelimitedStatement() {
        val text = """
            Date	Description	Debit	Credit
            13/04/2026	Amazon UPI	499.00	
            14/04/2026	Salary Credit		50000.00
        """.trimIndent()
        
        val parsed = parser.parseText(sourceName = "tab.csv", rawText = text, preferDelimited = true)
        
        assertEquals(2, parsed.entries.size)
    }

    @Test
    fun parsesFreeformStatementLines() {
        val text = """
            Statement Summary - April 2026
            Opening Balance: 10,000.00
            13-04-2026 AMAZON INDIA 499.00
            14-04-2026 SWIGGY FOOD 150.00
            Closing Balance: 9,351.00
        """.trimIndent()
        
        val parsed = parser.parseText(sourceName = "freeform.txt", rawText = text)
        
        assertEquals(2, parsed.entries.size)
        assertEquals(TransactionType.EXPENSE, parsed.entries[0].direction)
    }

    @Test
    fun parsesNegativeAmountsAsIncome() {
        val text = """
            Date,Description,Amount
            13/04/2026,Refund from Amazon,(499.00)
            14/04/2026,Shopping,599.00
        """.trimIndent()
        
        val parsed = parser.parseText(sourceName = "negative.csv", rawText = text, preferDelimited = true)
        
        assertEquals(2, parsed.entries.size)
        assertEquals(TransactionType.INCOME, parsed.entries[0].direction)
        assertEquals(TransactionType.EXPENSE, parsed.entries[1].direction)
    }

    @Test
    fun parsesSbiCardFormat() {
        val text = """
            Transaction Date,Posting Date,Description,Amount,Type
            01/04/2026,01/04/2026,AMAZON INDIA LT,1499.00,DR
            02/04/2026,02/04/2026,SWIGGY REFUND,-150.00,CR
            03/04/2026,03/04/2026,SALARY CREDIT,50000.00,CR
        """.trimIndent()
        
        val parsed = parser.parseText(sourceName = "SBI_Statement.csv", rawText = text, preferDelimited = true)
        
        assertEquals(3, parsed.entries.size)
        assertEquals(TransactionType.EXPENSE, parsed.entries[0].direction)
        assertEquals(TransactionType.INCOME, parsed.entries[1].direction)
        assertEquals(TransactionType.INCOME, parsed.entries[2].direction)
    }

    @Test
    fun parsesExtractedSbiCardPdfTextWithSingleLetterMarkers() {
        val text = """
            for Statement Period: 08 Mar 26 to 07 Apr 26
            Date AmountTransaction Details
            08 Mar 26 WALLET LOAD FEE DB    (EXCL TAX    6.46) 35.87 D
            10 Mar 26 PAYMENT RECEIVED 000000000CHD569J1EC2ZAQ 53,297.00 C
            16 Mar 26 FUEL SURCHARGE WAIVER EXCL TAX 10.00 C
            07 Mar 26 UPI-K501 KFC Golden I mNoida 397.95 D
            08 Mar 26 UPI-VASIM 100.00 D
            08 Mar 26 AMAZONPAYINDIAPRIVAT                 IN 3,587.50 D
            07 Apr 26 FP EMI 03/03(EXCL TAX    5.99) 2,415.02 M
            07 Apr 26 INTEREST ON EMI 33.25 D
            CGST DB @ 09.00% 62.96 D
            SGST DB @ 09.00% 62.96 D
        """.trimIndent()

        val parsed = parser.parseText(sourceName = "sbi-card-extracted.txt", rawText = text)

        assertEquals(10, parsed.entries.size)
        assertEquals(1, parsed.ignoredLineCount)

        assertEquals("WALLET LOAD FEE DB (EXCL TAX 6.46)", parsed.entries[0].description)
        assertEquals(3_587L, parsed.entries[0].amountMinor)
        assertEquals(TransactionType.EXPENSE, parsed.entries[0].direction)

        assertEquals("PAYMENT RECEIVED 000000000CHD569J1EC2ZAQ", parsed.entries[1].description)
        assertEquals(5_329_700L, parsed.entries[1].amountMinor)
        assertEquals(TransactionType.INCOME, parsed.entries[1].direction)

        assertEquals("FUEL SURCHARGE WAIVER EXCL TAX", parsed.entries[2].description)
        assertEquals(1_000L, parsed.entries[2].amountMinor)
        assertEquals(TransactionType.INCOME, parsed.entries[2].direction)

        assertEquals("FP EMI 03/03(EXCL TAX 5.99)", parsed.entries[6].description)
        assertEquals(241_502L, parsed.entries[6].amountMinor)
        assertEquals(TransactionType.EXPENSE, parsed.entries[6].direction)

        assertEquals("CGST DB @ 09.00%", parsed.entries[8].description)
        assertEquals(6_296L, parsed.entries[8].amountMinor)
        assertEquals(TransactionType.EXPENSE, parsed.entries[8].direction)
        assertEquals(parsed.entries[6].transactionTime.toLocalDate(), parsed.entries[8].transactionTime.toLocalDate())
    }

    @Test
    fun parsesStatementPdfStyleExtractWithoutHeaderFalsePositives() {
        val text = """
            Account Summary
            As on 13-04-2026
            STATEMENT OF ACCOUNT
            Statement From : 01-03-2026 to 31-03-2026
            Balance
            01/03/2026  01/03/2026  WDL TFR                     -           69.00            -  1,728.23
            UPI/DR/606043409272/Google
            A/utib/playstoreg/UPI
            05/03/2026 05/03/2026 UPI/DR/102852835642/SPOTIFY
            02/03/2026 02/03/2026 UPI/CR/642704539109/ALICE                    250.00         -    6,108.23
            25/03/2026 25/03/2026 INTEREST CREDIT                 -           -             2,000.00
            Statement Summary : 01-03-2026 To 31-03-2026
        """.trimIndent()

        val parsed = parser.parseText(
            sourceName = "statement-pdf-extract.txt",
            rawText = text
        )

        assertEquals(3, parsed.entries.size)
        assertEquals(6_900L, parsed.entries[0].amountMinor)
        assertEquals(TransactionType.EXPENSE, parsed.entries[0].direction)
        assertEquals(25_000L, parsed.entries[1].amountMinor)
        assertEquals(TransactionType.INCOME, parsed.entries[1].direction)
        assertEquals(200_000L, parsed.entries[2].amountMinor)
        assertEquals(TransactionType.INCOME, parsed.entries[2].direction)
        assertTrue(parsed.entries.none { it.description.contains("Account Summary", ignoreCase = true) })
        assertTrue(parsed.entries.none { it.description.contains("SPOTIFY", ignoreCase = true) })
    }

    private fun createPasswordProtectedPdf(password: String): ByteArray {
        return PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)

            val permissions = AccessPermission().apply {
                setCanExtractContent(true)
                setCanExtractForAccessibility(true)
            }
            val protection = StandardProtectionPolicy(
                "owner-$password",
                password,
                permissions
            ).apply {
                setEncryptionKeyLength(128)
                setPreferAES(true)
            }
            document.protect(protection)

            ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }
    }
}
