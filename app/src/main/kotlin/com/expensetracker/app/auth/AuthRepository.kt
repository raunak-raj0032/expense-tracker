package com.expensetracker.app.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.expensetracker.app.R
import com.expensetracker.app.di.FirebaseAuthService
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuthService: FirebaseAuthService
) {
    private val credentialManager = CredentialManager.create(context)

    val authState: Flow<AuthState> = firebaseAuthService.auth?.let { auth ->
        firebaseAuthState(auth)
    } ?: callbackFlow {
        trySend(AuthState.SignedOut)
        awaitClose { }
    }

    private fun firebaseAuthState(firebaseAuth: FirebaseAuth): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(
                if (user == null) {
                    AuthState.SignedOut
                } else {
                    AuthState.SignedIn(
                        AuthUser(
                            uid = user.uid,
                            displayName = user.displayName,
                            email = user.email,
                            photoUrl = user.photoUrl?.toString(),
                            isAnonymous = user.isAnonymous
                        )
                    )
                }
            )
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(activityContext: Context) {
        val firebaseAuth = firebaseAuthService.auth ?: throw firebaseNotConfigured()
        val webClientId = context.getString(R.string.default_web_client_id)
        if (webClientId.isBlank()) throw firebaseNotConfigured()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(generateNonce())
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            credentialManager.getCredential(activityContext, request)
        } catch (e: GetCredentialException) {
            throw AuthException(e.message ?: "Sign-in cancelled", e)
        }

        val credential = result.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException("Unexpected credential type: ${credential.type}")
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        firebaseAuth.signInWithCredential(firebaseCredential).await()
    }

    suspend fun signInAsGuest() {
        val firebaseAuth = firebaseAuthService.auth ?: throw firebaseNotConfigured()
        try {
            firebaseAuth.signInAnonymously().await()
        } catch (t: Throwable) {
            throw AuthException(friendlyAuthMessage(t), t)
        }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        val firebaseAuth = firebaseAuthService.auth ?: throw firebaseNotConfigured()
        val e = email.trim()
        if (e.isEmpty() || password.isEmpty()) {
            throw AuthException("Enter your email and password")
        }
        try {
            firebaseAuth.signInWithEmailAndPassword(e, password).await()
        } catch (t: Throwable) {
            throw AuthException(friendlyAuthMessage(t), t)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String?) {
        val firebaseAuth = firebaseAuthService.auth ?: throw firebaseNotConfigured()
        val e = email.trim()
        if (e.isEmpty() || password.isEmpty()) {
            throw AuthException("Enter your email and password")
        }
        if (password.length < 6) {
            throw AuthException("Password must be at least 6 characters")
        }
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(e, password).await()
            val name = displayName?.trim().orEmpty()
            if (name.isNotEmpty()) {
                val update = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                result.user?.updateProfile(update)?.await()
                firebaseAuth.currentUser?.reload()?.await()
            }
        } catch (t: Throwable) {
            throw AuthException(friendlyAuthMessage(t), t)
        }
    }

    suspend fun sendPasswordReset(email: String) {
        val firebaseAuth = firebaseAuthService.auth ?: throw firebaseNotConfigured()
        val e = email.trim()
        if (e.isEmpty()) throw AuthException("Enter your email first")
        try {
            firebaseAuth.sendPasswordResetEmail(e).await()
        } catch (t: Throwable) {
            throw AuthException(friendlyAuthMessage(t), t)
        }
    }

    suspend fun signOut() {
        firebaseAuthService.auth?.signOut()
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun friendlyAuthMessage(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                raw.contains("wrong-password", ignoreCase = true) ->
                "Incorrect email or password"
            raw.contains("no user record", ignoreCase = true) ||
                raw.contains("user-not-found", ignoreCase = true) ->
                "No account found for this email"
            raw.contains("email address is already", ignoreCase = true) ||
                raw.contains("email-already-in-use", ignoreCase = true) ->
                "An account already exists for this email"
            raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                raw.contains("admin-restricted-operation", ignoreCase = true) ->
                "Guest sign-in is not enabled in Firebase Auth"
            raw.contains("badly formatted", ignoreCase = true) ||
                raw.contains("invalid-email", ignoreCase = true) ->
                "That email address doesn't look right"
            raw.contains("network", ignoreCase = true) ->
                "Network error - check your connection"
            raw.isBlank() -> "Something went wrong, please try again"
            else -> raw
        }
    }

    private fun firebaseNotConfigured(): AuthException =
        AuthException("Firebase Auth is not configured. Place google-services.json in app/ and rebuild.")

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
