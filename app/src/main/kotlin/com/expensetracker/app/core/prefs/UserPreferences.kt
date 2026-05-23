package com.expensetracker.app.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.expensetracker.app.ai.AiBackend
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AiModelState(
    val enabled: Boolean,
    val backend: AiBackend,
    val lastError: String,
    val endpoint: String,
    val modelName: String,
    val localModelVersion: String,
    val localModelPath: String
)

const val DEFAULT_AI_MODEL_NAME: String = "llama3.2:3b"

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val onboardingKey = booleanPreferencesKey("onboarding_seen")
    private val biometricKey = booleanPreferencesKey("biometric_enabled")
    private val homeCurrencyKey = stringPreferencesKey("home_currency")
    private val aiEnabledKey = booleanPreferencesKey("ai_enabled")
    private val aiBackendKey = stringPreferencesKey("ai_backend")
    private val aiLastErrorKey = stringPreferencesKey("ai_last_error")
    private val aiEndpointKey = stringPreferencesKey("ai_endpoint")
    private val aiModelNameKey = stringPreferencesKey("ai_model_name")
    private val aiLocalModelVersionKey = stringPreferencesKey("ai_local_model_version")
    private val aiLocalModelPathKey = stringPreferencesKey("ai_local_model_path")
    private val budgetNotifEnabledKey = booleanPreferencesKey("budget_notif_enabled")
    private val budgetNotifPeriodKey = stringPreferencesKey("budget_notif_period")
    private val budgetWidgetPeriodKey = stringPreferencesKey("budget_widget_period")
    private val firstRunPermissionsPromptedKey = booleanPreferencesKey("first_run_permissions_prompted")
    private val tutorialSeenKey = booleanPreferencesKey("tutorial_seen")
    private val profilePicturePathKey = stringPreferencesKey("profile_picture_path")
    private val hfTokenKey = stringPreferencesKey("hf_token")

    val onboardingSeen: Flow<Boolean> = context.userPrefsDataStore.data.map { it[onboardingKey] ?: false }
    val biometricEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map { it[biometricKey] ?: false }
    val homeCurrency: Flow<String> = context.userPrefsDataStore.data.map { it[homeCurrencyKey] ?: "INR" }
    val budgetNotifEnabled: Flow<Boolean> = context.userPrefsDataStore.data.map { it[budgetNotifEnabledKey] ?: false }
    val budgetNotifPeriod: Flow<String> = context.userPrefsDataStore.data.map { it[budgetNotifPeriodKey] ?: "MONTHLY" }
    val budgetWidgetPeriod: Flow<String> = context.userPrefsDataStore.data.map { it[budgetWidgetPeriodKey] ?: "MONTHLY" }
    val firstRunPermissionsPrompted: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[firstRunPermissionsPromptedKey] ?: false }
    val tutorialSeen: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[tutorialSeenKey] ?: false }
    val profilePicturePath: Flow<String> = context.userPrefsDataStore.data.map { it[profilePicturePathKey].orEmpty() }
    val hfToken: Flow<String> = context.userPrefsDataStore.data.map { it[hfTokenKey].orEmpty() }

    val aiModelState: Flow<AiModelState> = context.userPrefsDataStore.data.map { prefs ->
        val endpoint = prefs[aiEndpointKey].orEmpty()
        AiModelState(
            enabled = prefs[aiEnabledKey] ?: false,
            backend = AiBackend.fromStoredName(prefs[aiBackendKey])
                ?: if (endpoint.isNotBlank()) AiBackend.OLLAMA else AiBackend.LOCAL,
            lastError = prefs[aiLastErrorKey].orEmpty(),
            endpoint = endpoint,
            modelName = prefs[aiModelNameKey] ?: DEFAULT_AI_MODEL_NAME,
            localModelVersion = prefs[aiLocalModelVersionKey].orEmpty(),
            localModelPath = prefs[aiLocalModelPathKey].orEmpty()
        )
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.userPrefsDataStore.edit { it[onboardingKey] = seen }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[biometricKey] = enabled }
    }

    suspend fun setHomeCurrency(code: String) {
        context.userPrefsDataStore.edit { it[homeCurrencyKey] = code.uppercase() }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[aiEnabledKey] = enabled }
    }

    suspend fun setAiBackend(backend: AiBackend) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[aiBackendKey] = backend.storedName
            prefs.remove(aiLastErrorKey)
        }
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

    suspend fun setLocalAiModelInstalled(version: String, path: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[aiLocalModelVersionKey] = version
            prefs[aiLocalModelPathKey] = path
            prefs.remove(aiLastErrorKey)
        }
    }

    suspend fun clearLocalAiModelState() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(aiLocalModelVersionKey)
            prefs.remove(aiLocalModelPathKey)
            prefs.remove(aiLastErrorKey)
            prefs[aiEnabledKey] = false
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

    suspend fun setFirstRunPermissionsPrompted(prompted: Boolean) {
        context.userPrefsDataStore.edit { it[firstRunPermissionsPromptedKey] = prompted }
    }

    suspend fun setTutorialSeen(seen: Boolean) {
        context.userPrefsDataStore.edit { it[tutorialSeenKey] = seen }
    }

    suspend fun setHfToken(token: String) {
        context.userPrefsDataStore.edit { prefs ->
            if (token.isBlank()) prefs.remove(hfTokenKey) else prefs[hfTokenKey] = token.trim()
        }
    }

    suspend fun setProfilePicturePath(path: String) {
        context.userPrefsDataStore.edit { prefs ->
            if (path.isEmpty()) {
                prefs.remove(profilePicturePathKey)
            } else {
                prefs[profilePicturePathKey] = path
            }
        }
    }

    suspend fun backupSnapshot(): Map<String, Any> {
        val prefs = context.userPrefsDataStore.data.first()
        return buildMap {
            put("onboarding_seen", prefs[onboardingKey] ?: false)
            put("biometric_enabled", prefs[biometricKey] ?: false)
            put("home_currency", prefs[homeCurrencyKey] ?: "INR")
            put("ai_enabled", prefs[aiEnabledKey] ?: false)
            put("ai_backend", prefs[aiBackendKey] ?: AiBackend.LOCAL.storedName)
            put("ai_last_error", prefs[aiLastErrorKey].orEmpty())
            put("ai_endpoint", prefs[aiEndpointKey].orEmpty())
            put("ai_model_name", prefs[aiModelNameKey] ?: DEFAULT_AI_MODEL_NAME)
            put("ai_local_model_version", prefs[aiLocalModelVersionKey].orEmpty())
            put("ai_local_model_path", prefs[aiLocalModelPathKey].orEmpty())
            put("budget_notif_enabled", prefs[budgetNotifEnabledKey] ?: false)
            put("budget_notif_period", prefs[budgetNotifPeriodKey] ?: "MONTHLY")
            put("budget_widget_period", prefs[budgetWidgetPeriodKey] ?: "MONTHLY")
            put("first_run_permissions_prompted", prefs[firstRunPermissionsPromptedKey] ?: false)
            put("tutorial_seen", prefs[tutorialSeenKey] ?: false)
            put("profile_picture_path", prefs[profilePicturePathKey].orEmpty())
        }
    }

    suspend fun restoreBackupSnapshot(values: Map<String, Any?>) {
        context.userPrefsDataStore.edit { prefs ->
            values["onboarding_seen"]?.let { prefs[onboardingKey] = it as Boolean }
            values["biometric_enabled"]?.let { prefs[biometricKey] = it as Boolean }
            values["home_currency"]?.let { prefs[homeCurrencyKey] = it as String }
            values["ai_enabled"]?.let { prefs[aiEnabledKey] = it as Boolean }
            values["ai_backend"]?.let { prefs[aiBackendKey] = it as String }
            values["ai_last_error"]?.let { prefs[aiLastErrorKey] = it as String }
            values["ai_endpoint"]?.let { prefs[aiEndpointKey] = it as String }
            values["ai_model_name"]?.let { prefs[aiModelNameKey] = it as String }
            values["ai_local_model_version"]?.let { prefs[aiLocalModelVersionKey] = it as String }
            values["ai_local_model_path"]?.let { prefs[aiLocalModelPathKey] = it as String }
            values["budget_notif_enabled"]?.let { prefs[budgetNotifEnabledKey] = it as Boolean }
            values["budget_notif_period"]?.let { prefs[budgetNotifPeriodKey] = it as String }
            values["budget_widget_period"]?.let { prefs[budgetWidgetPeriodKey] = it as String }
            values["first_run_permissions_prompted"]?.let { prefs[firstRunPermissionsPromptedKey] = it as Boolean }
            values["tutorial_seen"]?.let { prefs[tutorialSeenKey] = it as Boolean }
            values["profile_picture_path"]?.let { prefs[profilePicturePathKey] = it as String }
        }
    }

    suspend fun clearAiModelState() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(aiLastErrorKey)
            prefs.remove(aiEndpointKey)
            prefs.remove(aiLocalModelVersionKey)
            prefs.remove(aiLocalModelPathKey)
            prefs[aiEnabledKey] = false
        }
    }
}
