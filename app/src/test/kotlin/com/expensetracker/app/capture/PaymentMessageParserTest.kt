package com.expensetracker.app.capture

import com.expensetracker.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("Amazon Pay", result.merchant)
        assertEquals("Transfer", result.categoryHint)
        assertEquals("UPI", result.paymentMethod)
        assertEquals("123ABC456DEF", result.reference)
        assertEquals("Paid to Amazon Pay", result.description)
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

    @Test
    fun parsesAmazonShoppingPaymentSeparatelyFromWallet() {
        val result = parser.parse(
            sender = "AX-AMZPAY",
            text = "Rs 899 paid to AMAZON using UPI Ref 123456ABCDEF."
        )

        assertTrue(result.isTransaction)
        assertEquals(89_900L, result.amountMinor)
        assertEquals(TransactionType.EXPENSE, result.direction)
        assertEquals("Amazon", result.merchant)
        assertEquals("Shopping", result.categoryHint)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parsesAmazonPayWalletTopUpAsTransfer() {
        val result = parser.parse(
            sender = "VM-AMZPAY",
            text = "Added money of Rs 500.00 to Amazon Pay balance via UPI Ref 556677889900."
        )

        assertTrue(result.isTransaction)
        assertEquals(50_000L, result.amountMinor)
        assertEquals(TransactionType.TRANSFER, result.direction)
        assertEquals("Amazon Pay Balance", result.merchant)
        assertEquals("Transfer", result.categoryHint)
        assertEquals("Wallet", result.paymentMethod)
    }

    @Test
    fun parsesBankDebitSmsWithAvailableBalance() {
        val result = parser.parse(
            sender = "VK-HDFCBK",
            text = "Alert: A/c XX1234 debited by Rs.1,234.56 at ZOMATO on 07-May-26. UTR 612345678901. Avl Bal Rs.9,876.00"
        )

        assertTrue(result.isTransaction)
        assertEquals(123_456L, result.amountMinor)
        assertEquals(TransactionType.EXPENSE, result.direction)
        assertEquals("Zomato", result.merchant)
        assertEquals("Food", result.categoryHint)
        assertEquals("612345678901", result.reference)
    }

    @Test
    fun parsesUpiAccessibilitySuccessScreen() {
        val result = parser.parse(
            packageName = "com.phonepe.app",
            text = "Payment successful ₹75 Paid to Fresh Mart Banking name: FRESH MART INDIA UPI transaction ID 612345678901"
        )

        assertTrue(result.isTransaction)
        assertEquals(7_500L, result.amountMinor)
        assertEquals(TransactionType.EXPENSE, result.direction)
        assertEquals("Fresh Mart", result.merchant)
        assertEquals("UPI", result.paymentMethod)
        assertEquals("612345678901", result.reference)
    }

    @Test
    fun ignoresFailedOrPendingPaymentRequests() {
        val failed = parser.parse(
            packageName = "com.google.android.apps.nbu.paisa.user",
            text = "Payment failed for ₹250 to Rahul Sharma. Please try again."
        )
        val collectRequest = parser.parse(
            text = "Collect request received from shop@upi for INR 400. Approve with UPI PIN."
        )

        assertFalse(failed.isTransaction)
        assertFalse(collectRequest.isTransaction)
    }
}
