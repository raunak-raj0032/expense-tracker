package com.expensetracker.app.capture

import com.expensetracker.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentMessageParserTest {

    private val parser = PaymentMessageParser()

    @Test
    fun parsesPeerToPeerUpiSend() {
        val result = parser.parse(
            sender = "VK-HDFCBK",
            text = "Rs.250.00 paid to Rahul Sharma via UPI Ref No 123456789012."
        )

        assertTrue(result.isTransaction)
        assertEquals(25_000L, result.amountMinor)
        assertEquals(TransactionType.EXPENSE, result.direction)
        assertEquals("Rahul Sharma", result.merchant)
        assertEquals("UPI", result.paymentMethod)
        assertEquals("Transfer", result.categoryHint)
        assertTrue(result.isPeerTransfer)
        assertEquals("Sent to Rahul Sharma", result.description)
    }

    @Test
    fun parsesShoppingPaymentAndCategory() {
        val result = parser.parse(
            sender = "AD-AXISBK",
            text = "INR 499.00 debited from A/c XX1234 for UPI to AMAZONPAY on 13-04-2026 UTR 123ABC456DEF."
        )

        assertTrue(result.isTransaction)
        assertEquals(49_900L, result.amountMinor)
        assertEquals(TransactionType.EXPENSE, result.direction)
        assertEquals("Amazon", result.merchant)
        assertEquals("Shopping", result.categoryHint)
        assertEquals("UPI", result.paymentMethod)
        assertEquals("123ABC456DEF", result.reference)
        assertEquals("Paid to Amazon", result.description)
    }

    @Test
    fun parsesUpiIncomeMessage() {
        val result = parser.parse(
            sender = "VM-SBIUPI",
            text = "Rs 1500 received from Neha via UPI Ref 998877665544."
        )

        assertTrue(result.isTransaction)
        assertEquals(150_000L, result.amountMinor)
        assertEquals(TransactionType.INCOME, result.direction)
        assertEquals("Neha", result.merchant)
        assertEquals("Transfer", result.categoryHint)
        assertEquals("Received from Neha", result.description)
        assertNotNull(result.fingerprint)
    }
}
