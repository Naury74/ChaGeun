package com.naury.chageun.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.weather.LastWeather
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherSnapshot
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class PreferencesLastWeatherRepositoryTest {

    private val directory: File = Files.createTempDirectory("prefs").toFile()
    private val scope = TestScope(UnconfinedTestDispatcher())
    private val dataStore =
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(directory, "test.preferences_pb") }
    private val repository = PreferencesLastWeatherRepository(dataStore)

    private val weather = LastWeather(
        snapshot = WeatherSnapshot(
            WeatherCondition.Rain,
            17.5,
            4.2,
            isNight = true,
            observedAt = Instant.parse("2026-10-12T09:00:00Z"),
        ),
        fetchedAt = Instant.parse("2026-10-12T09:12:30Z"),
        areaName = "서울 강남구",
        locationKey = "Current",
    )

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun savesAndLoads_withoutAnyCoordinate() = scope.runTest {
        assertThat(repository.load()).isNull()

        repository.save(weather)

        assertThat(repository.load()).isEqualTo(weather)
        // 저장한 값은 날씨·시각·동네 이름·위치 설정뿐이다. 위도·경도는 없다.
        val stored = dataStore.data.first().asMap().keys.map { it.name }
        assertThat(stored.none { "lat" in it || "lon" in it || "point" in it }).isTrue()
    }

    @Test
    fun regionWeather_hasNoAreaName_andClearRemovesEverything() = scope.runTest {
        repository.save(weather)
        repository.save(weather.copy(areaName = null, locationKey = "Region(region=Seoul)"))
        assertThat(repository.load()?.areaName).isNull()

        repository.clear()

        assertThat(repository.load()).isNull()
        assertThat(dataStore.data.first().asMap()).isEmpty()
    }
}
