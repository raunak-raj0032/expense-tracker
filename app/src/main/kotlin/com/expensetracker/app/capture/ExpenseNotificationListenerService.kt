package com.expensetracker.app.capture

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExpenseNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var captureEventRepository: CaptureEventRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val SUPPORTED_PACKAGES = setOf(
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
            "com.whatsapp",
            "com.axisbank.digibank",
            "com.icici.bank.imobile",
            "com.hdfcbank.mobilebanking",
            "com.sbi.lionmobileservice",
            "com.yesbank",
            "com.kotak.bank.mobile",
            "com.csam.icici.bank.imobile"
        )

        private val SIGNAL_KEYWORDS = listOf(
            "debited",
            "credited",
            "spent",
            "received",
            "paid",
            "payment",
            "withdrawn",
            "deposit",
            "upi",
            "vpa",
            "txn",
            "transaction",
            "imps",
            "neft",
            "rtgs"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (!shouldInspect(sbn)) {
            return
        }

        serviceScope.launch {
            try {
                processNotification(sbn)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        activeNotifications
            ?.filter(::shouldInspect)
            ?.forEach { notification ->
                serviceScope.launch {
                    try {
                        processNotification(notification)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: Bundle.EMPTY
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = buildNotificationBody(extras)
        val subtext = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        ).joinToString(" ").trim()

        if (title.isBlank() && text.isBlank() && subtext.isBlank()) {
            return
        }

        captureEventRepository.captureNotification(
            packageName = sbn.packageName,
            title = title,
            text = text,
            subtext = subtext
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }

    private fun shouldInspect(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) {
            return false
        }

        if (SUPPORTED_PACKAGES.contains(sbn.packageName)) {
            return true
        }

        val extras = sbn.notification.extras ?: Bundle.EMPTY
        val searchableText = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString(),
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.joinToString(" ") { it.toString() }
        ).joinToString(" ").lowercase()

        return SIGNAL_KEYWORDS.any(searchableText::contains)
    }

    private fun buildNotificationBody(extras: Bundle): String {
        return listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString(),
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.joinToString(" ") { it.toString() }
        )
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .trim()
    }
}
