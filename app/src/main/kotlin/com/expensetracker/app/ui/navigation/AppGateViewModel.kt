package com.expensetracker.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppGateViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    val onboardingSeen: StateFlow<Boolean?> = userPreferences.onboardingSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val homeCurrency: StateFlow<String> = userPreferences.homeCurrency
        .stateIn(viewModelScope, SharingStarted.Eagerly, "INR")

    val firstRunPermissionsPrompted: StateFlow<Boolean> =
        userPreferences.firstRunPermissionsPrompted
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val tutorialSeen: StateFlow<Boolean> =
        userPreferences.tutorialSeen
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun markFirstRunPermissionsPrompted() {
        viewModelScope.launch {
            userPreferences.setFirstRunPermissionsPrompted(true)
        }
    }

    fun markTutorialSeen() {
        viewModelScope.launch { userPreferences.setTutorialSeen(true) }
    }

    fun replayTutorial() {
        viewModelScope.launch { userPreferences.setTutorialSeen(false) }
    }
}
