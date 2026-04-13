package com.expensetracker.app.capture

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
            "com.google.android.apps.nbu.paisa.provider",
            "com.phonepe.app",
            "com.paytm.app",
            "in.org.npci.bhimapp",
            "com.axisbank.digibank",
            "com.icici.bank.imobile",
            "com.hdfcbank.mobilebanking",
            "com.sbi.lionmobileservice",
            "com.yesbank"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (!SUPPORTED_PACKAGES.contains(sbn.packageName)) {
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

    private suspend fun processNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        captureEventRepository.captureNotification(
            packageName = sbn.packageName,
            title = extras.getCharSequence("android.title")?.toString() ?: "",
            text = extras.getCharSequence("android.text")?.toString() ?: "",
            subtext = extras.getCharSequence("android.subText")?.toString() ?: ""
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }
}
