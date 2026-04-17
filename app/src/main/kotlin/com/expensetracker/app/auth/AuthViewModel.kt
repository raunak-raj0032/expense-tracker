package com.expensetracker.app.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
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
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            authState.collect { state ->
                val wm = WorkManager.getInstance(appContext)
                when (state) {
                    is AuthState.SignedIn -> {
                        TransactionSyncWorker.enqueuePeriodic(wm)
                        TransactionSyncWorker.enqueueOneShot(wm)
                    }
                    AuthState.SignedOut -> TransactionSyncWorker.cancel(wm)
                    AuthState.Loading -> Unit
                }
            }
        }
    }

    fun signIn(activityContext: Context) {
        if (_signingIn.value) return
        _signingIn.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching { repository.signInWithGoogle(activityContext) }
                .onFailure { _error.value = it.message ?: "Sign-in failed" }
            _signingIn.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun clearError() { _error.value = null }
}
