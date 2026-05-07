package com.expensetracker.app.core.device

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun getDeviceId(): String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ).orEmpty()
}
