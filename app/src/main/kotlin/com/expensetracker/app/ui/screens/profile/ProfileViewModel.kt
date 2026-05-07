package com.expensetracker.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> = userPreferences.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val profilePicturePath: StateFlow<String> = userPreferences.profilePicturePath
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _cameraImageUri = MutableStateFlow<Uri?>(null)
    val cameraImageUri: StateFlow<Uri?> = _cameraImageUri.asStateFlow()

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBiometricEnabled(enabled) }
    }

    fun setProfilePicturePath(path: String) {
        viewModelScope.launch { userPreferences.setProfilePicturePath(path) }
    }

    fun setProfilePictureUri(uri: Uri, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            val path = saveProfilePicture(uri)
            withContext(Dispatchers.Main) { onComplete(path) }
        }
    }

    fun createCameraImageUri(): Uri? {
        return try {
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return null
            if (!picturesDir.exists()) picturesDir.mkdirs()
            val tempFile = File(picturesDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
            _cameraImageUri.value = uri
            uri
        } catch (e: Exception) {
            null
        }
    }

    fun savePhotoFromCamera(onComplete: (String?) -> Unit) {
        val uri = _cameraImageUri.value ?: run { onComplete(null); return }
        viewModelScope.launch {
            val path = saveProfilePicture(uri)
            _cameraImageUri.value = null
            withContext(Dispatchers.Main) { onComplete(path) }
        }
    }

    fun cancelCameraCapture() {
        val uri = _cameraImageUri.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {}
            _cameraImageUri.value = null
        }
    }

    fun removeProfilePicture() {
        val path = profilePicturePath.value
        viewModelScope.launch(Dispatchers.IO) {
            if (path.isNotEmpty()) {
                try { File(path).delete() } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) { setProfilePicturePath("") }
        }
    }

    private suspend fun saveProfilePicture(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val profileDir = File(context.filesDir, "profile_pictures")
            if (!profileDir.exists()) profileDir.mkdirs()
            val prev = profilePicturePath.value
            if (prev.isNotEmpty()) {
                try { File(prev).delete() } catch (_: Exception) {}
            }
            val newFile = File(profileDir, "profile_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(newFile).use { output -> input.copyTo(output) }
            }
            newFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
