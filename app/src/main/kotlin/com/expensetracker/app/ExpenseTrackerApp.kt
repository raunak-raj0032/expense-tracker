package com.expensetracker.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.expensetracker.app.core.notif.NotificationChannels
import com.expensetracker.app.diagnostics.AppDiagnostics
import com.expensetracker.app.work.WorkerScheduler
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppDiagnostics.init(applicationContext)
        PDFBoxResourceLoader.init(applicationContext)
        NotificationChannels.create(this)
        WorkerScheduler.scheduleAll(this)
    }
}
