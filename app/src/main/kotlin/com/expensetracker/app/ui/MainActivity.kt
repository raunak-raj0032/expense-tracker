package com.expensetracker.app.ui

import android.os.Bundle
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
import com.expensetracker.app.auth.BiometricGate
import com.expensetracker.app.ui.navigation.MainNavigation
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            onDarkThemeChange = { darkThemeEnabled = it }
                        )
                    }
                }
            }
        }
    }
}
