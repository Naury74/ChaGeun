package com.naury.chageun.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import com.naury.chageun.core.model.KoreaRegion
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 위치는 "current" 또는 시·도 이름으로만 저장한다. 좌표는 어디에도 남기지 않는다. */
internal class PreferencesWeatherSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : WeatherSettingsRepository {

    override val settings: Flow<WeatherSettings> = dataStore.data.map { prefs ->
        WeatherSettings(
            isEnabled = prefs[ENABLED] ?: true,
            location = prefs[LOCATION]?.let(::decode),
        )
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[ENABLED] = enabled }
    }

    override suspend fun setLocation(location: WeatherLocation) {
        dataStore.edit {
            it[LOCATION] = encode(location)
            // 위치를 고르는 것은 날씨를 보겠다는 뜻이라 꺼 둔 경우에도 다시 켠다.
            it[ENABLED] = true
        }
    }

    private fun encode(location: WeatherLocation): String = when (location) {
        WeatherLocation.Current -> CURRENT
        is WeatherLocation.Region -> location.region.name
    }

    private fun decode(stored: String): WeatherLocation? = when (stored) {
        CURRENT -> WeatherLocation.Current
        else -> KoreaRegion.entries.firstOrNull { it.name == stored }?.let(WeatherLocation::Region)
    }

    private companion object {
        const val CURRENT = "current"
        val ENABLED = booleanPreferencesKey("weather_enabled")
        val LOCATION = stringPreferencesKey("weather_location")
    }
}
