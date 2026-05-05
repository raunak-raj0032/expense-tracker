package com.expensetracker.app.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val homeCurrencyKey = stringPreferencesKey("home_currency")
    private val aiEnabledKey = booleanPreferencesKey("ai_enabled")
    private val aiLastErrorKey = stringPreferencesKey("ai_last_error")
    private val aiEndpointKey = stringPreferencesKey("ai_endpoint")
    private val aiModelNameKey = stringPreferencesKey("ai_model_name")
    private val budgetNotifEnabledKey = booleanPreferencesKey("budget_notif_enabled")
    private val budgetNotifPeriodKey = stringPreferencesKey("budget_notif_period")
    private val budgetWidgetPeriodKey = stringPreferencesKey("budget_widget_period")
    private val firstRunPermissionsPromptedKey = booleanPreferencesKey("first_run_permissions_prompted")
    private val tutorialSeenKey = booleanPreferencesKey("tutorial_seen")
    private val backupEmailKey = stringPreferencesKey("backup_email")
    private val profilePicturePathKey = stringPreferencesKey("profile_picture_path")

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
    val backupEmail: Flow<String> = context.userPrefsDataStore.data.map { it[backupEmailKey].orEmpty() }
    val profilePicturePath: Flow<String> = context.userPrefsDataStore.data.map { it[profilePicturePathKey].orEmpty() }

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

    suspend fun setHomeCurrency(code: String) {
        context.userPrefsDataStore.edit { it[homeCurrencyKey] = code.uppercase() }
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

    suspend fun setFirstRunPermissionsPrompted(prompted: Boolean) {
        context.userPrefsDataStore.edit { it[firstRunPermissionsPromptedKey] = prompted }
    }

    suspend fun setTutorialSeen(seen: Boolean) {
        context.userPrefsDataStore.edit { it[tutorialSeenKey] = seen }
    }

    suspend fun setBackupEmail(email: String) {
        context.userPrefsDataStore.edit { prefs ->
            val cleaned = email.trim()
            if (cleaned.isEmpty()) {
                prefs.remove(backupEmailKey)
            } else {
                prefs[backupEmailKey] = cleaned
            }
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
            put("ai_last_error", prefs[aiLastErrorKey].orEmpty())
            put("ai_endpoint", prefs[aiEndpointKey].orEmpty())
            put("ai_model_name", prefs[aiModelNameKey] ?: DEFAULT_AI_MODEL_NAME)
            put("budget_notif_enabled", prefs[budgetNotifEnabledKey] ?: false)
            put("budget_notif_period", prefs[budgetNotifPeriodKey] ?: "MONTHLY")
            put("budget_widget_period", prefs[budgetWidgetPeriodKey] ?: "MONTHLY")
            put("first_run_permissions_prompted", prefs[firstRunPermissionsPromptedKey] ?: false)
            put("tutorial_seen", prefs[tutorialSeenKey] ?: false)
            put("backup_email", prefs[backupEmailKey].orEmpty())
            put("profile_picture_path", prefs[profilePicturePathKey].orEmpty())
        }
    }

    suspend fun restoreBackupSnapshot(values: Map<String, Any?>) {
        context.userPrefsDataStore.edit { prefs ->
            values["onboarding_seen"]?.let { prefs[onboardingKey] = it as Boolean }
            values["biometric_enabled"]?.let { prefs[biometricKey] = it as Boolean }
            values["home_currency"]?.let { prefs[homeCurrencyKey] = it as String }
            values["ai_enabled"]?.let { prefs[aiEnabledKey] = it as Boolean }
            values["ai_last_error"]?.let { prefs[aiLastErrorKey] = it as String }
            values["ai_endpoint"]?.let { prefs[aiEndpointKey] = it as String }
            values["ai_model_name"]?.let { prefs[aiModelNameKey] = it as String }
            values["budget_notif_enabled"]?.let { prefs[budgetNotifEnabledKey] = it as Boolean }
            values["budget_notif_period"]?.let { prefs[budgetNotifPeriodKey] = it as String }
            values["budget_widget_period"]?.let { prefs[budgetWidgetPeriodKey] = it as String }
            values["first_run_permissions_prompted"]?.let { prefs[firstRunPermissionsPromptedKey] = it as Boolean }
            values["tutorial_seen"]?.let { prefs[tutorialSeenKey] = it as Boolean }
            values["backup_email"]?.let { prefs[backupEmailKey] = it as String }
            values["profile_picture_path"]?.let { prefs[profilePicturePathKey] = it as String }
        }
    }

    suspend fun clearAiModelState() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(aiLastErrorKey)
            prefs.remove(aiEndpointKey)
        }
    }
}
