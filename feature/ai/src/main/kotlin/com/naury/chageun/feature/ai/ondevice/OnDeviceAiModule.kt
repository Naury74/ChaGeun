package com.naury.chageun.feature.ai.ondevice

import com.naury.chageun.core.domain.ai.OnDeviceAssistant
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface OnDeviceAiModule {
    @Binds
    fun bindOnDeviceAssistant(assistant: MlKitOnDeviceAssistant): OnDeviceAssistant
}
