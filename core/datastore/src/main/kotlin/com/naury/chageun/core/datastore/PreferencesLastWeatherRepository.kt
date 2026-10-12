package com.naury.chageun.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.naury.chageun.core.domain.weather.LastWeather
import com.naury.chageun.core.domain.weather.LastWeatherRepository
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherSnapshot
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 마지막 날씨를 설정과 같은 파일에 둔다. 날씨 상태·기온·바람·받은 시각과 동네 이름만 담고 좌표는 남기지 않는다.
 * 한 값이라도 없거나 읽을 수 없으면 저장된 날씨가 없는 것으로 본다.
 */
internal class PreferencesLastWeatherRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    LastWeatherRepository {

    override suspend fun load(): LastWeather? {
        val prefs = dataStore.data.first()
        val condition = prefs[CONDITION]?.let { name -> WeatherCondition.entries.firstOrNull { it.name == name } }
        val temperature = prefs[TEMPERATURE]
        val wind = prefs[WIND]
        val observedAt = prefs[OBSERVED_AT]
        val fetchedAt = prefs[FETCHED_AT]
        val locationKey = prefs[LOCATION_KEY]
        if (condition == null || temperature == null || wind == null) return null
        if (observedAt == null || fetchedAt == null || locationKey == null) return null
        return LastWeather(
            snapshot = WeatherSnapshot(
                condition = condition,
                temperatureC = temperature,
                windSpeedMs = wind,
                isNight = prefs[NIGHT] ?: false,
                observedAt = Instant.ofEpochMilli(observedAt),
            ),
            fetchedAt = Instant.ofEpochMilli(fetchedAt),
            areaName = prefs[AREA_NAME],
            locationKey = locationKey,
        )
    }

    override suspend fun save(weather: LastWeather) {
        dataStore.edit {
            it[CONDITION] = weather.snapshot.condition.name
            it[TEMPERATURE] = weather.snapshot.temperatureC
            it[WIND] = weather.snapshot.windSpeedMs
            it[NIGHT] = weather.snapshot.isNight
            it[OBSERVED_AT] = weather.snapshot.observedAt.toEpochMilli()
            it[FETCHED_AT] = weather.fetchedAt.toEpochMilli()
            it[LOCATION_KEY] = weather.locationKey
            val areaName = weather.areaName
            if (areaName != null) it[AREA_NAME] = areaName else it.remove(AREA_NAME)
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.removeLastWeather() }
    }

    private fun MutablePreferences.removeLastWeather() {
        listOf(CONDITION, TEMPERATURE, WIND, NIGHT, OBSERVED_AT, FETCHED_AT, AREA_NAME, LOCATION_KEY).forEach {
            remove(it)
        }
    }

    private companion object {
        val CONDITION = stringPreferencesKey("last_weather_condition")
        val TEMPERATURE = doublePreferencesKey("last_weather_temperature_c")
        val WIND = doublePreferencesKey("last_weather_wind_ms")
        val NIGHT = booleanPreferencesKey("last_weather_is_night")
        val OBSERVED_AT = longPreferencesKey("last_weather_observed_at")
        val FETCHED_AT = longPreferencesKey("last_weather_fetched_at")
        val AREA_NAME = stringPreferencesKey("last_weather_area_name")
        val LOCATION_KEY = stringPreferencesKey("last_weather_location")
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface LastWeatherBindingModule {
    @Binds
    fun bindLastWeatherRepository(repository: PreferencesLastWeatherRepository): LastWeatherRepository
}
