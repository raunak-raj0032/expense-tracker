package com.expensetracker.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppGateViewModel @Inject constructor(
    userPreferences: UserPreferences
) : ViewModel() {
    val onboardingSeen: StateFlow<Boolean?> = userPreferences.onboardingSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val homeCurrency: StateFlow<String> = userPreferences.homeCurrency
        .stateIn(viewModelScope, SharingStarted.Eagerly, "INR")
}
