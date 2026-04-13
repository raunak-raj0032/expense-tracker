package com.expensetracker.app.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExpenseSmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var captureEventRepository: CaptureEventRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val groupedMessages = messages.groupBy { "${it.originatingAddress}|${it.timestampMillis}" }

                groupedMessages.values.forEach { parts ->
                    val sender = parts.firstOrNull()?.originatingAddress
                    val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
                    val receivedAt = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    captureEventRepository.captureSms(
                        sender = sender,
                        body = body,
                        receivedAt = receivedAt
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
