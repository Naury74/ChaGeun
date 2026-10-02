package com.naury.chageun.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.naury.chageun.core.domain.reminder.MileageReminderLog
import com.naury.chageun.core.domain.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_settings") }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsBindingModule {
    @Binds
    fun bindSettingsRepository(repository: PreferencesSettingsRepository): SettingsRepository

    @Binds
    fun bindMileageReminderLog(log: PreferencesMileageReminderLog): MileageReminderLog
}
