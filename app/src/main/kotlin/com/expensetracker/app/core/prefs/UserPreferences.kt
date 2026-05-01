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
    val lastError: String,
    val endpoint: String,
    val modelName: String
)

const val DEFAULT_AI_MODEL_NAME: String = "llama3.2:3b"

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
    private val aiLastErrorKey = stringPreferencesKey("ai_last_error")
    private val aiEndpointKey = stringPreferencesKey("ai_endpoint")
    private val aiModelNameKey = stringPreferencesKey("ai_model_name")
    private val budgetNotifEnabledKey = booleanPreferencesKey("budget_notif_enabled")
    private val budgetNotifPeriodKey = stringPreferencesKey("budget_notif_period")
    private val budgetWidgetPeriodKey = stringPreferencesKey("budget_widget_period")

    val onboardingSeen: Flow<Boolean> = context.userPrefsDataStore.data.map { it[onboardingKey] ?: false }
    val biometricEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map { it[biometricKey] ?: false }
    val lastSyncMillis: Flow<Long> = context.userPrefsDataStore.data.map { it[lastSyncKey] ?: 0L }
    val homeCurrency: Flow<String> = context.userPrefsDataStore.data.map { it[homeCurrencyKey] ?: "INR" }
    val budgetNotifEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map { it[budgetNotifEnabledKey] ?: false }
    val budgetNotifPeriod: Flow<String> = context.userPrefsDataStore.data.map { it[budgetNotifPeriodKey] ?: "MONTHLY" }
    val budgetWidgetPeriod: Flow<String> = context.userPrefsDataStore.data.map { it[budgetWidgetPeriodKey] ?: "MONTHLY" }

    val aiModelState: Flow<AiModelState> = context.userPrefsDataStore.data.map { prefs ->
        AiModelState(
            enabled = prefs[aiEnabledKey] ?: false,
            lastError = prefs[aiLastErrorKey].orEmpty(),
            endpoint = prefs[aiEndpointKey].orEmpty(),
            modelName = prefs[aiModelNameKey] ?: DEFAULT_AI_MODEL_NAME
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

    suspend fun setAiLastError(message: String) {
        context.userPrefsDataStore.edit { it[aiLastErrorKey] = message }
    }

    suspend fun setAiEndpoint(endpoint: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[aiEndpointKey] = endpoint.trim().trimEnd('/')
            prefs.remove(aiLastErrorKey)
        }
    }

    suspend fun setAiModelName(modelName: String) {
        val cleaned = modelName.trim()
        context.userPrefsDataStore.edit { prefs ->
            if (cleaned.isEmpty()) {
                prefs.remove(aiModelNameKey)
            } else {
                prefs[aiModelNameKey] = cleaned
            }
            prefs.remove(aiLastErrorKey)
        }
    }

    suspend fun setBudgetNotifEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[budgetNotifEnabledKey] = enabled }
    }

    suspend fun setBudgetNotifPeriod(period: String) {
        context.userPrefsDataStore.edit { it[budgetNotifPeriodKey] = period }
    }

    suspend fun setBudgetWidgetPeriod(period: String) {
        context.userPrefsDataStore.edit { it[budgetWidgetPeriodKey] = period }
    }

    suspend fun clearAiModelState() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(aiLastErrorKey)
            prefs.remove(aiEndpointKey)
        }
    }
}
