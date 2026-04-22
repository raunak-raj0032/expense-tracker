package com.expensetracker.app.ui.screens.settings

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.ai.AiAvailability
import com.expensetracker.app.ai.OnDeviceAiManager
import com.expensetracker.app.core.data.repository.AppDataRepository
import com.expensetracker.app.core.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appDataRepository: AppDataRepository,
    private val userPreferences: UserPreferences,
    private val onDeviceAiManager: OnDeviceAiManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val homeCurrency: StateFlow<String> = userPreferences.homeCurrency
        .stateIn(viewModelScope, SharingStarted.Eagerly, "INR")

    val aiAvailability: StateFlow<AiAvailability> = onDeviceAiManager.observeAvailability()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiAvailability.Initializing)

    val aiEnabled: StateFlow<Boolean> = userPreferences.aiModelState
        .map { it.enabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val aiDownloadedBytes: StateFlow<Long> = userPreferences.aiModelState
        .map { it.downloadedBytes }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAiEnabled(enabled)
            _message.value = if (enabled) "On-device AI enabled." else "On-device AI disabled."
        }
    }

    fun downloadAiModel() {
        if (_aiBusy.value) return
        _aiBusy.value = true
        viewModelScope.launch {
            onDeviceAiManager.downloadModel()
                .onFailure { _message.value = it.message ?: "Model download failed." }
                .onSuccess { _message.value = "Model download complete." }
            _aiBusy.value = false
        }
    }

    fun deleteAiModel() {
        if (_aiBusy.value) return
        _aiBusy.value = true
        viewModelScope.launch {
            onDeviceAiManager.deleteModel()
            _message.value = "On-device model removed."
            _aiBusy.value = false
        }
    }

    fun setHomeCurrency(code: String) {
        viewModelScope.launch {
            userPreferences.setHomeCurrency(code)
            _message.value = "Home currency set to $code."
        }
    }

    private val _resettingData = MutableStateFlow(false)
    val resettingData: StateFlow<Boolean> = _resettingData.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !canUseBiometric()) {
                _message.value = "Biometric unlock is not available on this device."
                return@launch
            }
            userPreferences.setBiometricEnabled(enabled)
            _message.value = if (enabled) {
                "App lock enabled for face or fingerprint."
            } else {
                "App lock disabled."
            }
        }
    }

    fun deleteAllEntries() {
        if (_resettingData.value) return
        _resettingData.value = true
        viewModelScope.launch {
            runCatching { appDataRepository.deleteAllEntries() }
                .onSuccess {
                    _message.value = "All local entries were deleted."
                }
                .onFailure {
                    _message.value = it.message ?: "Failed to delete local entries."
                }
            _resettingData.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun canUseBiometric(): Boolean {
        val result = BiometricManager.from(appContext).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}
