package com.expensetracker.app.core.money

import java.util.Currency
import java.util.Locale

object CurrencyConverter {
    private val ratesToInr = mapOf(
        "INR" to 1.0,
        "USD" to 83.5,
        "EUR" to 91.0,
        "GBP" to 106.0,
        "JPY" to 0.56,
        "AUD" to 55.0,
        "CAD" to 61.0,
        "SGD" to 62.0,
        "AED" to 22.7,
        "CNY" to 11.5,
        "CHF" to 94.0
    )

    val supported: List<String> = ratesToInr.keys.toList().sorted()

    fun convert(amountMinor: Long, from: String, to: String): Long {
        if (from.equals(to, ignoreCase = true)) return amountMinor
        val fromRate = ratesToInr[from.uppercase()] ?: return amountMinor
        val toRate = ratesToInr[to.uppercase()] ?: return amountMinor
        val inInr = amountMinor * fromRate
        return (inInr / toRate).toLong()
    }

    fun symbol(currencyCode: String): String = try {
        Currency.getInstance(currencyCode.uppercase()).getSymbol(Locale.getDefault())
    } catch (_: Throwable) {
        currencyCode
    }

    fun formatAmount(amountMinor: Long, currencyCode: String = "INR"): String {
        val sym = symbol(currencyCode)
        val whole = amountMinor / 100
        val frac = amountMinor % 100
        val tail = if (frac > 0) ".${frac.toString().padStart(2, '0')}" else ""
        return "$sym$whole$tail"
    }
}
