package com.naury.chageun.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.naury.chageun.core.datastore.PreferencesLastWeatherRepository.Companion.removeLastWeather
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionRepository
import com.naury.chageun.core.domain.weather.RenamedRegionCodes
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 위치는 "current" 또는 고른 지역의 행정구역코드(10자리)로만 저장한다. 좌표는 어디에도 남기지 않는다.
 * "현재 위치"의 동네 이름(시·군·구까지)은 다음 실행 때 바로 보이도록 두지만, 끄거나 위치를 바꾸면 지운다.
 *
 * 예전 버전은 시·도를 영어 이름(Busan)으로 저장했다. [WeatherRegionMigration]이 처음 읽을 때 코드로 바꾸고,
 * 그 전에 읽히더라도 여기서 같은 표로 풀어 준다.
 */
internal class PreferencesWeatherSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val regions: KoreaRegionRepository,
) : WeatherSettingsRepository {

    // 동네 이름처럼 다른 키만 바뀌었을 때 날씨를 다시 불러오지 않게 같은 설정은 한 번만 내보낸다.
    // 지역 목록은 지역을 고른 경우에만 읽는다. 새 지역 목록을 받으면 같은 코드도 새 목록으로 다시 푼다.
    override val settings: Flow<WeatherSettings> = combine(
        dataStore.data,
        regions.listInfo.map { it.version }.distinctUntilChanged(),
    ) { prefs, _ ->
        WeatherSettings(
            isEnabled = prefs[ENABLED] ?: true,
            location = prefs[LOCATION]?.let { decode(it) },
            isPromptDismissed = prefs[PROMPT_DISMISSED] ?: false,
        )
    }.distinctUntilChanged()

    override val recentRegionCodes: Flow<List<String>> = dataStore.data
        .map { prefs -> prefs[RECENT_REGIONS]?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty() }
        .distinctUntilChanged()

    override val currentAreaName: Flow<String?> = dataStore.data.map { it[AREA_NAME] }.distinctUntilChanged()

    override val isCurrentLocationMissing: Flow<Boolean> =
        dataStore.data.map { it[LOCATION_MISSING] ?: false }.distinctUntilChanged()

    override val isRegionChangeNoticeVisible: Flow<Boolean> =
        dataStore.data.map { it[ENABLED] != false && it[REGION_CHANGED] == true }.distinctUntilChanged()

    override suspend fun dismissRegionChangeNotice() {
        dataStore.edit { it.remove(REGION_CHANGED) }
    }

    override suspend fun applyRegionListChange(
        version: String,
        renamedCodes: Map<String, String>,
        catalog: KoreaRegionCatalog,
    ) {
        dataStore.edit {
            val applied = it[REGION_LIST_VERSION]
            if (applied != null && applied >= version) return@edit
            it[REGION_LIST_VERSION] = version
            it[RECENT_REGIONS]?.let { stored ->
                it[RECENT_REGIONS] = stored.split(SEPARATOR).filter(String::isNotEmpty)
                    .map { code -> RenamedRegionCodes.follow(code, renamedCodes) }
                    .distinct()
                    .joinToString(SEPARATOR)
            }
            val stored = it[LOCATION]?.takeIf { location -> location != CURRENT } ?: return@edit
            val code = RenamedRegionCodes.follow(WeatherRegionMigration.codeFor(stored), renamedCodes)
            if (code != stored) {
                it[LOCATION] = code
                it.removeLastWeather()
            }
            if (catalog.find(code) == null) it[REGION_CHANGED] = true
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit {
            it[ENABLED] = enabled
            if (!enabled) {
                it.remove(AREA_NAME)
                it.remove(LOCATION_MISSING)
                it.removeLastWeather()
            }
        }
    }

    override suspend fun setLocation(location: WeatherLocation) {
        dataStore.edit {
            val encoded = encode(location)
            // 다른 위치로 바꾸면 예전 위치에서 받은 마지막 날씨는 맞지 않으니 바로 지운다.
            if (it[LOCATION] != encoded) it.removeLastWeather()
            it[LOCATION] = encoded
            if (location is WeatherLocation.Region) {
                it[RECENT_REGIONS] =
                    withRecent(it[RECENT_REGIONS], location.area.code)
            }
            // 위치를 고르는 것은 날씨를 보겠다는 뜻이라 꺼 둔 경우에도 다시 켠다.
            it[ENABLED] = true
            // 위치를 다시 고르면 예전 동네 이름은 맞지 않을 수 있다. 다음 날씨를 받을 때 새로 찾는다.
            it.remove(AREA_NAME)
            it.remove(LOCATION_MISSING)
            it.remove(REGION_CHANGED)
        }
    }

    override suspend fun clearRecentRegions() {
        dataStore.edit { it.remove(RECENT_REGIONS) }
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
        is WeatherLocation.Region -> location.area.code
    }

    private suspend fun decode(stored: String): WeatherLocation? = when (stored) {
        CURRENT -> WeatherLocation.Current
        // 목록이 바뀌어 코드가 없어졌으면 위 시·군·구나 시·도로 보여 준다.
        else -> regions.catalog().resolve(WeatherRegionMigration.codeFor(stored))?.let(WeatherLocation::Region)
    }

    private fun withRecent(stored: String?, code: String): String {
        val previous = stored?.split(SEPARATOR).orEmpty().filter { it.isNotEmpty() && it != code }
        return (listOf(code) + previous).take(MAX_RECENTS).joinToString(SEPARATOR)
    }

    private companion object {
        const val CURRENT = WeatherRegionMigration.CURRENT
        const val SEPARATOR = ","
        const val MAX_RECENTS = 5
        val RECENT_REGIONS = stringPreferencesKey("weather_recent_regions")
        val ENABLED = booleanPreferencesKey("weather_enabled")
        val LOCATION = WeatherRegionMigration.LOCATION
        val PROMPT_DISMISSED = booleanPreferencesKey("weather_prompt_dismissed")
        val AREA_NAME = stringPreferencesKey("weather_current_area_name")
        val LOCATION_MISSING = booleanPreferencesKey("weather_current_location_missing")
        val REGION_CHANGED = booleanPreferencesKey("weather_region_changed")

        // 고른 지역 코드를 마지막으로 맞춰 둔 지역 목록 판. 같은 판으로 두 번 바꾸지 않는다.
        val REGION_LIST_VERSION = stringPreferencesKey("weather_region_list_version")
    }
}
