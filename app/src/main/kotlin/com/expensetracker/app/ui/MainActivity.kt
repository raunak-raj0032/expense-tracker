package com.expensetracker.app.ui

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.expensetracker.app.core.notif.LastActiveTracker
import com.expensetracker.app.diagnostics.AppDiagnostics
import com.expensetracker.app.auth.BiometricGate
import com.expensetracker.app.ui.navigation.MainNavigation
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var lastActiveTracker: LastActiveTracker

    private var captureInboxRequest by mutableStateOf(0)
    private var notificationRouteRequest by mutableStateOf(0)
    private var notificationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDiagnostics.log("MainActivity onCreate action=${intent?.action} route=${intent?.getStringExtra(EXTRA_NAV_ROUTE)}")
        recordCaptureInboxRequest(intent)
        setContent {
            var darkThemeEnabled by rememberSaveable { mutableStateOf(true) }

            ExpenseTrackerTheme(darkTheme = darkThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiometricGate {
                        MainNavigation(
                            darkThemeEnabled = darkThemeEnabled,
                            onDarkThemeChange = { darkThemeEnabled = it },
                            captureInboxRequest = captureInboxRequest,
                            notificationRoute = notificationRoute,
                            notificationRouteRequest = notificationRouteRequest
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lastActiveTracker.updateLastAppOpen()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppDiagnostics.log("MainActivity onNewIntent action=${intent.action} route=${intent.getStringExtra(EXTRA_NAV_ROUTE)}")
        setIntent(intent)
        recordCaptureInboxRequest(intent)
    }

    private fun recordCaptureInboxRequest(intent: Intent?) {
        if (intent?.hasExtra("capture_event_id") == true) {
            AppDiagnostics.log("Capture inbox requested from intent")
            captureInboxRequest += 1
        }
        intent?.getStringExtra(EXTRA_NAV_ROUTE)?.let { route ->
            AppDiagnostics.log("Notification route requested: $route")
            notificationRoute = route
            notificationRouteRequest += 1
        }
    }

    companion object {
        const val EXTRA_NAV_ROUTE = "com.expensetracker.app.extra.NAV_ROUTE"
    }
}
