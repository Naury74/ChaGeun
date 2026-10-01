package com.naury.chageun.logging

import com.naury.chageun.core.common.logging.AppLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface LoggingModule {
    @Binds
    @Singleton
    fun bindAppLogger(logger: LogcatAppLogger): AppLogger
}
