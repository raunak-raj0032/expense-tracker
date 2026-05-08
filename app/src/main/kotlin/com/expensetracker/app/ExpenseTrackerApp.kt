package com.expensetracker.app

import android.app.Application
import com.expensetracker.app.diagnostics.AppDiagnostics
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ExpenseTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDiagnostics.init(applicationContext)
        PDFBoxResourceLoader.init(applicationContext)
    }
}
