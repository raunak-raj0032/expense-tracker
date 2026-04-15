package com.expensetracker.app.statement

import com.expensetracker.app.capture.PaymentMessageParser
import com.expensetracker.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

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
}
