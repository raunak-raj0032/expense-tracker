package com.expensetracker.app.capture

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationParserRegistry @Inject constructor(
    private val paymentMessageParser: PaymentMessageParser
) {

    fun parse(packageName: String, title: String, text: String, subtext: String): ParseResult {
        return paymentMessageParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            subtext = subtext
        )
    }

    fun parseSms(sender: String?, body: String): ParseResult {
        return paymentMessageParser.parse(
            sender = sender,
            text = body
        )
    }
}
