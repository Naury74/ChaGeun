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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 위치는 "current" 또는 시·도 이름으로만 저장한다. 좌표는 어디에도 남기지 않는다.
 * "현재 위치"의 동네 이름(시·군·구까지)은 다음 실행 때 바로 보이도록 두지만, 끄거나 위치를 바꾸면 지운다.
 */
internal class PreferencesWeatherSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : WeatherSettingsRepository {

    // 동네 이름처럼 다른 키만 바뀌었을 때 날씨를 다시 불러오지 않게 같은 설정은 한 번만 내보낸다.
    override val settings: Flow<WeatherSettings> = dataStore.data.map { prefs ->
        WeatherSettings(
            isEnabled = prefs[ENABLED] ?: true,
            location = prefs[LOCATION]?.let(::decode),
            isPromptDismissed = prefs[PROMPT_DISMISSED] ?: false,
        )
    }.distinctUntilChanged()

    override val currentAreaName: Flow<String?> = dataStore.data.map { it[AREA_NAME] }.distinctUntilChanged()

    override val isCurrentLocationMissing: Flow<Boolean> =
        dataStore.data.map { it[LOCATION_MISSING] ?: false }.distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit {
            it[ENABLED] = enabled
            if (!enabled) {
                it.remove(AREA_NAME)
                it.remove(LOCATION_MISSING)
            }
        }
    }

    override suspend fun setLocation(location: WeatherLocation) {
        dataStore.edit {
            it[LOCATION] = encode(location)
            // 위치를 고르는 것은 날씨를 보겠다는 뜻이라 꺼 둔 경우에도 다시 켠다.
            it[ENABLED] = true
            // 위치를 다시 고르면 예전 동네 이름은 맞지 않을 수 있다. 다음 날씨를 받을 때 새로 찾는다.
            it.remove(AREA_NAME)
            it.remove(LOCATION_MISSING)
        }
    }

    override suspend fun dismissPrompt() {
        dataStore.edit { it[PROMPT_DISMISSED] = true }
    }

    override suspend fun setCurrentAreaName(name: String?) {
        dataStore.edit {
            // 찾는 사이에 날씨를 끄거나 지역을 직접 골랐으면 저장하지 않는다.
            val isCurrent = it[ENABLED] != false && it[LOCATION] == CURRENT
            if (name != null && isCurrent) it[AREA_NAME] = name else it.remove(AREA_NAME)
        }
    }

    override suspend fun setCurrentLocationMissing(missing: Boolean) {
        dataStore.edit {
            val isCurrent = it[ENABLED] != false && it[LOCATION] == CURRENT
            if (missing && isCurrent) it[LOCATION_MISSING] = true else it.remove(LOCATION_MISSING)
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
        val PROMPT_DISMISSED = booleanPreferencesKey("weather_prompt_dismissed")
        val AREA_NAME = stringPreferencesKey("weather_current_area_name")
        val LOCATION_MISSING = booleanPreferencesKey("weather_current_location_missing")
    }
}
