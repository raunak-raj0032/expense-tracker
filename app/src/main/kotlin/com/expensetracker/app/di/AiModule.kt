package com.expensetracker.app.di

import com.expensetracker.app.ai.OnDeviceAiManager
import com.expensetracker.app.ai.StubOnDeviceAiManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindOnDeviceAiManager(impl: StubOnDeviceAiManager): OnDeviceAiManager
}
