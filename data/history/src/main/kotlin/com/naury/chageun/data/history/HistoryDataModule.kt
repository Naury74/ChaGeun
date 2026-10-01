package com.naury.chageun.data.history

import com.naury.chageun.core.domain.history.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface HistoryDataModule {
    @Binds
    fun bindHistoryRepository(repository: OfflineFirstHistoryRepository): HistoryRepository
}
