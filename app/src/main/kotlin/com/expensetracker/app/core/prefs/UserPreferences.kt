package com.expensetracker.app.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AiModelState(
    val enabled: Boolean,
    val version: String,
    val path: String,
    val downloadedBytes: Long,
    val lastError: String
)

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val onboardingKey = booleanPreferencesKey("onboarding_seen")
    private val biometricKey = booleanPreferencesKey("biometric_enabled")
    private val lastSyncKey = longPreferencesKey("last_sync_millis")
    private val homeCurrencyKey = stringPreferencesKey("home_currency")
    private val aiEnabledKey = booleanPreferencesKey("ai_enabled")
    private val aiModelVersionKey = stringPreferencesKey("ai_model_version")
    private val aiModelPathKey = stringPreferencesKey("ai_model_path")
    private val aiModelDownloadedBytesKey = longPreferencesKey("ai_model_downloaded_bytes")
    private val aiLastErrorKey = stringPreferencesKey("ai_last_error")

    val onboardingSeen: Flow<Boolean> = context.userPrefsDataStore.data.map { it[onboardingKey] ?: false }
    val biometricEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map { it[biometricKey] ?: false }
    val lastSyncMillis: Flow<Long> = context.userPrefsDataStore.data.map { it[lastSyncKey] ?: 0L }
    val homeCurrency: Flow<String> = context.userPrefsDataStore.data.map { it[homeCurrencyKey] ?: "INR" }

    val aiModelState: Flow<AiModelState> = context.userPrefsDataStore.data.map { prefs ->
        AiModelState(
            enabled = prefs[aiEnabledKey] ?: false,
            version = prefs[aiModelVersionKey].orEmpty(),
            path = prefs[aiModelPathKey].orEmpty(),
            downloadedBytes = prefs[aiModelDownloadedBytesKey] ?: 0L,
            lastError = prefs[aiLastErrorKey].orEmpty()
        )
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.userPrefsDataStore.edit { it[onboardingKey] = seen }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[biometricKey] = enabled }
    }

    suspend fun setLastSyncMillis(millis: Long) {
        context.userPrefsDataStore.edit { it[lastSyncKey] = millis }
    }

    suspend fun setHomeCurrency(code: String) {
        context.userPrefsDataStore.edit { it[homeCurrencyKey] = code.uppercase() }
    }

    suspend fun clearSyncState() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(lastSyncKey)
        }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[aiEnabledKey] = enabled }
    }

    suspend fun setAiModelInstalled(version: String, path: String, sizeBytes: Long) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[aiModelVersionKey] = version
            prefs[aiModelPathKey] = path
            prefs[aiModelDownloadedBytesKey] = sizeBytes
            prefs.remove(aiLastErrorKey)
        }
    }

    suspend fun setAiLastError(message: String) {
        context.userPrefsDataStore.edit { it[aiLastErrorKey] = message }
    }

    suspend fun clearAiModelState() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(aiModelVersionKey)
            prefs.remove(aiModelPathKey)
            prefs.remove(aiModelDownloadedBytesKey)
            prefs.remove(aiLastErrorKey)
        }
    }
}
