package com.expensetracker.app.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _emailBusy = MutableStateFlow(false)
    val emailBusy: StateFlow<Boolean> = _emailBusy.asStateFlow()

    private val _info = MutableStateFlow<String?>(null)
    val info: StateFlow<String?> = _info.asStateFlow()

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

    fun signInAsGuest() {
        if (_signingIn.value) return
        _signingIn.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching { repository.signInAsGuest() }
                .onFailure { _error.value = it.message ?: "Guest sign-in failed" }
            _signingIn.value = false
        }
    }

    fun signInWithEmail(email: String, password: String) {
        if (_emailBusy.value) return
        _emailBusy.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching { repository.signInWithEmail(email, password) }
                .onFailure { _error.value = it.message ?: "Sign-in failed" }
            _emailBusy.value = false
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String?) {
        if (_emailBusy.value) return
        _emailBusy.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching { repository.signUpWithEmail(email, password, displayName) }
                .onFailure { _error.value = it.message ?: "Sign-up failed" }
            _emailBusy.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        _error.value = null
        _info.value = null
        viewModelScope.launch {
            runCatching { repository.sendPasswordReset(email) }
                .onSuccess { _info.value = "Password reset email sent" }
                .onFailure { _error.value = it.message ?: "Could not send reset email" }
        }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun clearError() { _error.value = null }
    fun clearInfo() { _info.value = null }
}
