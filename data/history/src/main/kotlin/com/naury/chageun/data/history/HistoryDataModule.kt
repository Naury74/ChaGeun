package com.naury.chageun.data.history

import android.content.Context
import com.naury.chageun.core.common.storage.AttachmentDirectory
import com.naury.chageun.core.domain.history.AttachmentRepository
import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
internal interface HistoryDataModule {
    @Binds
    fun bindHistoryRepository(repository: OfflineFirstHistoryRepository): HistoryRepository

    @Binds
    fun bindAttachmentRepository(repository: OfflineFirstAttachmentRepository): AttachmentRepository

    @Binds
    fun bindVehiclePhotoRepository(repository: OfflineFirstVehiclePhotoRepository): VehiclePhotoRepository

    @Binds
    fun bindImageImporter(importer: BitmapImageImporter): ImageImporter
}

@Module
@InstallIn(SingletonComponent::class)
internal object AttachmentStorageModule {
    @Provides
    @AttachmentDirectory
    fun provideAttachmentDirectory(@ApplicationContext context: Context): File = File(context.filesDir, "attachments")
}
