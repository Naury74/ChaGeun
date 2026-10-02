package com.naury.chageun.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.ads.AppUsageRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PreferencesAppUsageRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    AppUsageRepository {

    override val usage: Flow<AppUsage> = dataStore.data.map { prefs ->
        AppUsage(
            firstLaunchAt = prefs[FIRST_LAUNCH_EPOCH_MS]?.let(Instant::ofEpochMilli),
            launchCount = prefs[LAUNCH_COUNT] ?: 0,
        )
    }

    override suspend fun recordLaunch(at: Instant) {
        dataStore.edit { prefs ->
            if (prefs[FIRST_LAUNCH_EPOCH_MS] == null) prefs[FIRST_LAUNCH_EPOCH_MS] = at.toEpochMilli()
            prefs[LAUNCH_COUNT] = (prefs[LAUNCH_COUNT] ?: 0) + 1
        }
    }

    private companion object {
        val FIRST_LAUNCH_EPOCH_MS = longPreferencesKey("first_launch_epoch_ms")
        val LAUNCH_COUNT = intPreferencesKey("launch_count")
    }
}
