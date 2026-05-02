package com.expensetracker.app.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

data class FirebaseAuthService(
    val auth: FirebaseAuth?
) {
    val isConfigured: Boolean
        get() = auth != null
}

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideFirebaseAuthService(
        @ApplicationContext context: Context
    ): FirebaseAuthService {
        val app = FirebaseApp.getApps(context).firstOrNull()
            ?: runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
            ?: return FirebaseAuthService(auth = null)

        return FirebaseAuthService(
            auth = runCatching { FirebaseAuth.getInstance(app) }.getOrNull()
        )
    }
}
