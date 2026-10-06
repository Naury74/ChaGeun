package com.naury.chageun.core.database.di

import android.content.Context
import androidx.room.Room
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChageunDatabase =
        Room.databaseBuilder(context, ChageunDatabase::class.java, ChageunDatabase.NAME)
            .apply { DatabaseMigrations.ALL.forEach(::addMigrations) }
            .build()

    @Provides
    fun provideVehicleDao(database: ChageunDatabase) = database.vehicleDao()

    @Provides
    fun provideMileageRecordDao(database: ChageunDatabase) = database.mileageRecordDao()

    @Provides
    fun provideMaintenanceDao(database: ChageunDatabase) = database.maintenanceDao()

    @Provides
    fun provideHistoryDao(database: ChageunDatabase) = database.historyDao()

    @Provides
    fun provideAttachmentDao(database: ChageunDatabase) = database.attachmentDao()

    @Provides
    fun provideAlbumDao(database: ChageunDatabase) = database.albumDao()

    @Provides
    fun provideReminderDao(database: ChageunDatabase) = database.reminderDao()

    @Provides
    fun provideBackupDao(database: ChageunDatabase) = database.backupDao()

    @Provides
    fun provideInspectionDao(database: ChageunDatabase) = database.inspectionDao()
}
