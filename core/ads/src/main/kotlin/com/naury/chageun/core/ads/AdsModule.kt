package com.naury.chageun.core.ads

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AdsModule {
    @Binds
    fun bindAdConsentController(manager: UmpAdConsentController): AdConsentController
}
