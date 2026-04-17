package com.expensetracker.app.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.expensetracker.app.core.prefs.UserPreferences
import com.expensetracker.app.sync.SyncResult
import com.expensetracker.app.sync.TransactionSyncRepository
import com.expensetracker.app.sync.TransactionSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val syncRepository: TransactionSyncRepository,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lastSyncMillis: StateFlow<Long> = userPreferences.lastSyncMillis
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBiometricEnabled(enabled) }
    }

    fun syncNow() {
        if (_syncing.value) return
        _syncing.value = true
        _syncMessage.value = null
        viewModelScope.launch {
            val push = syncRepository.pushAll()
            val pull = syncRepository.pullAll()
            _syncMessage.value = when {
                push is SyncResult.Failure -> "Sync failed: ${push.message}"
                pull is SyncResult.Failure -> "Sync failed: ${pull.message}"
                push is SyncResult.NotSignedIn || pull is SyncResult.NotSignedIn -> "Sign in required"
                else -> "Synced"
            }
            TransactionSyncWorker.enqueueOneShot(WorkManager.getInstance(appContext))
            _syncing.value = false
        }
    }

    fun clearMessage() { _syncMessage.value = null }
}
