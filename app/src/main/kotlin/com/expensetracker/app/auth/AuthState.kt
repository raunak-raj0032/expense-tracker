package com.expensetracker.app.auth

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
) {
    val firstName: String
        get() = displayName?.trim()?.split(' ')?.firstOrNull().orEmpty()
}

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}
