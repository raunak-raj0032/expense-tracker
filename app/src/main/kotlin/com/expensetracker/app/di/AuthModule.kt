package com.expensetracker.app.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

data class FirebaseServices(
    val auth: FirebaseAuth?,
    val firestore: FirebaseFirestore?
) {
    val isConfigured: Boolean
        get() = auth != null && firestore != null
}

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideFirebaseServices(
        @ApplicationContext context: Context
    ): FirebaseServices {
        val app = FirebaseApp.getApps(context).firstOrNull()
            ?: runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
            ?: return FirebaseServices(auth = null, firestore = null)

        return FirebaseServices(
            auth = runCatching { FirebaseAuth.getInstance(app) }.getOrNull(),
            firestore = runCatching { FirebaseFirestore.getInstance(app) }.getOrNull()
        )
    }
}
