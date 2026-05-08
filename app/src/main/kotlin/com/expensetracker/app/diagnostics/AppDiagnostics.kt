package com.expensetracker.app.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppDiagnostics {
    private const val LOG_DIR = "diagnostics"
    private const val LOG_FILE = "pocket-pulse-log.txt"
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile private var initialized = false
    private lateinit var appContext: Context
    private var previousCrashHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
        log("App started on Android ${Build.VERSION.RELEASE} (${Build.MODEL})")

        previousCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log("Uncaught crash on ${thread.name}\n${throwable.stackTraceString()}")
            previousCrashHandler?.uncaughtException(thread, throwable)
        }
    }

    fun log(message: String) {
        if (!initialized) return
        runCatching {
            val file = logFile(appContext)
            file.parentFile?.mkdirs()
            file.appendText("${timestampFormat.format(Date())}  $message\n")
            trimIfNeeded(file)
        }
    }

    fun logFile(context: Context): File = File(File(context.filesDir, LOG_DIR), LOG_FILE)

    fun hasLog(context: Context): Boolean = logFile(context).takeIf(File::exists)?.length()?.let { it > 0L } == true

    private fun trimIfNeeded(file: File) {
        val maxBytes = 256 * 1024
        if (file.length() <= maxBytes) return
        val text = file.readText()
        file.writeText(text.takeLast(maxBytes / 2))
    }

    private fun Throwable.stackTraceString(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
}
