package com.naury.chageun.flags

import com.naury.chageun.core.domain.flags.FeatureFlagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FeatureFlagModule {
    @Binds
    abstract fun bindFeatureFlagRepository(repository: DefaultFeatureFlagRepository): FeatureFlagRepository
}
