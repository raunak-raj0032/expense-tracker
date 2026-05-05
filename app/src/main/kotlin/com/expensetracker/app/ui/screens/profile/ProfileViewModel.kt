package com.expensetracker.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val profilePicturePath: StateFlow<String> = userPreferences.profilePicturePath
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBiometricEnabled(enabled) }
    }

    fun setProfilePicturePath(path: String) {
        viewModelScope.launch { userPreferences.setProfilePicturePath(path) }
    }

    fun setProfilePictureUri(uri: Uri, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            val path = saveProfilePicture(uri)
            withContext(Dispatchers.Main) {
                onComplete(path)
            }
        }
    }

    private suspend fun saveProfilePicture(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val profileDir = File(context.filesDir, "profile_pictures")
            if (!profileDir.exists()) {
                profileDir.mkdirs()
            }

            val previousPathValue = profilePicturePath.value
            if (previousPathValue.isNotEmpty()) {
                val previousFile = File(previousPathValue)
                if (previousFile.exists()) {
                    previousFile.delete()
                }
            }

            val newFile = File(profileDir, "profile_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(newFile).use { output ->
                    input.copyTo(output)
                }
            }
            newFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
