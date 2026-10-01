package com.naury.chageun.data.backup

import com.naury.chageun.core.domain.backup.BackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface BackupDataModule {
    @Binds
    fun bindBackupRepository(repository: RoomBackupRepository): BackupRepository
}
