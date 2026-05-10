package com.expensetracker.app.core.data.repository

import android.content.Context
import android.net.Uri
import com.expensetracker.app.di.FirebaseAuthService
import com.expensetracker.app.di.FirebaseStorageService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuthService: FirebaseAuthService,
    private val firebaseStorageService: FirebaseStorageService
) {
    suspend fun uploadProfilePicture(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuthService.auth
            val storage = firebaseStorageService.storage

            if (auth == null || storage == null) {
                return@withContext Result.failure(Exception("Firebase not configured"))
            }

            val user = auth.currentUser ?: return@withContext Result.failure(Exception("User not signed in"))

            val profileDir = File(context.filesDir, "profile_pictures")
            if (!profileDir.exists()) profileDir.mkdirs()
            val localFile = File(profileDir, "upload_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output -> input.copyTo(output) }
            }

            val storageRef = storage.reference.child("profile_pictures/${user.uid}/photo.jpg")
            storageRef.putFile(Uri.fromFile(localFile)).await()

            val downloadUrl = storageRef.downloadUrl.await().toString()

            val update = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setPhotoUri(android.net.Uri.parse(downloadUrl))
                .build()
            user.updateProfile(update).await()

            localFile.delete()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun removeProfilePicture(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuthService.auth
            val storage = firebaseStorageService.storage

            if (auth == null || storage == null) {
                return@withContext Result.failure(Exception("Firebase not configured"))
            }

            val user = auth.currentUser ?: return@withContext Result.failure(Exception("User not signed in"))

            val storageRef = storage.reference.child("profile_pictures/${user.uid}/photo.jpg")
            storageRef.delete().await()

            val update = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setPhotoUri(null)
                .build()
            user.updateProfile(update).await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getFirebasePhotoUrl(): String? {
        return firebaseAuthService.auth?.currentUser?.photoUrl?.toString()
    }
}