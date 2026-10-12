package com.naury.chageun.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.weather.LastWeather
import com.naury.chageun.core.domain.weather.LastWeatherCache
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.model.WeatherSnapshot
import com.naury.chageun.core.testing.FakeKoreaRegionRepository
import com.naury.chageun.core.testing.TestKoreaRegions
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class PreferencesWeatherSettingsRepositoryTest {

    private val directory: File = Files.createTempDirectory("prefs").toFile()
    private val scope = TestScope(UnconfinedTestDispatcher())
    private val dataStore =
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(directory, "test.preferences_pb") }
    private val regions = FakeKoreaRegionRepository()
    private val repository = PreferencesWeatherSettingsRepository(dataStore, regions)

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun defaultsToEnabled_withoutLocation() = scope.runTest {
        assertThat(repository.settings.first()).isEqualTo(WeatherSettings())
    }

    @Test
    fun storesRegionOrCurrent_andChoosingLocationTurnsWeatherBackOn() = scope.runTest {
        repository.setEnabled(false)
        repository.setLocation(WeatherLocation.Region(TestKoreaRegions.jeju))
        assertThat(repository.settings.first())
            .isEqualTo(WeatherSettings(isEnabled = true, location = WeatherLocation.Region(TestKoreaRegions.jeju)))

        repository.setLocation(WeatherLocation.Current)
        repository.setEnabled(false)
        assertThat(repository.settings.first())
            .isEqualTo(WeatherSettings(isEnabled = false, location = WeatherLocation.Current))
    }

    @Test
    fun areaName_isKeptForCurrentLocation_andClearedWhenOffOrLocationChanges() = scope.runTest {
        repository.setLocation(WeatherLocation.Current)
        repository.setCurrentAreaName("서울 강남구")
        assertThat(repository.currentAreaName.first()).isEqualTo("서울 강남구")

        repository.setEnabled(false)
        assertThat(repository.currentAreaName.first()).isNull()

        repository.setLocation(WeatherLocation.Current)
        repository.setCurrentAreaName("경기 성남시")
        repository.setLocation(WeatherLocation.Region(TestKoreaRegions.busan))
        assertThat(repository.currentAreaName.first()).isNull()

        // 지역을 직접 고른 뒤 늦게 도착한 이름은 저장하지 않는다.
        repository.setCurrentAreaName("서울 강남구")
        assertThat(repository.currentAreaName.first()).isNull()

        repository.setLocation(WeatherLocation.Current)
        repository.setCurrentAreaName("서울 강남구")
        repository.setCurrentAreaName(null)
        assertThat(repository.currentAreaName.first()).isNull()
    }

    @Test
    fun locationMissing_isKeptForCurrentOnly_andClearedWhenFoundOffOrChanged() = scope.runTest {
        repository.setLocation(WeatherLocation.Region(TestKoreaRegions.seoul))
        repository.setCurrentLocationMissing(true)
        assertThat(repository.isCurrentLocationMissing.first()).isFalse()

        repository.setLocation(WeatherLocation.Current)
        repository.setCurrentLocationMissing(true)
        assertThat(repository.isCurrentLocationMissing.first()).isTrue()

        repository.setCurrentLocationMissing(false)
        assertThat(repository.isCurrentLocationMissing.first()).isFalse()

        repository.setCurrentLocationMissing(true)
        repository.setEnabled(false)
        assertThat(repository.isCurrentLocationMissing.first()).isFalse()

        repository.setLocation(WeatherLocation.Current)
        repository.setCurrentLocationMissing(true)
        repository.setLocation(WeatherLocation.Region(TestKoreaRegions.jeju))
        assertThat(repository.isCurrentLocationMissing.first()).isFalse()
    }

    @Test
    fun storesOnlyTheRegionCode_andReadsItBackWithLabels() = scope.runTest {
        repository.setLocation(WeatherLocation.Region(TestKoreaRegions.yeoksam1))

        assertThat(dataStore.data.first()[LOCATION]).isEqualTo("1168064000")
        val location = repository.settings.first().location as WeatherLocation.Region
        assertThat(location.area.label).isEqualTo("강남구 역삼1동")
        assertThat(location.area.path).isEqualTo("서울 강남구 역삼1동")
    }

    @Test
    fun currentLocation_doesNotLoadTheRegionList() = scope.runTest {
        repository.setLocation(WeatherLocation.Current)
        repository.settings.first()

        assertThat(regions.loads).isEqualTo(0)
    }

    @Test
    fun legacyProvinceName_isStillReadAsTheMatchingRegion() = scope.runTest {
        // 예전 버전이 남긴 값. 마이그레이션 전에 읽혀도 같은 지역으로 보인다.
        dataStore.edit { it[LOCATION] = "Busan" }

        assertThat(repository.settings.first().location).isEqualTo(WeatherLocation.Region(TestKoreaRegions.busan))
    }

    @Test
    fun migration_rewritesLegacyProvinceNamesToCodes() = scope.runTest {
        val legacy = preferencesOf(LOCATION to "Gwangju")
        assertThat(WeatherRegionMigration.shouldMigrate(legacy)).isTrue()
        assertThat(WeatherRegionMigration.migrate(legacy)[LOCATION]).isEqualTo("1224000000")
        assertThat(WeatherRegionMigration.migrate(preferencesOf(LOCATION to "Jeju"))[LOCATION])
            .isEqualTo("5000000000")

        assertThat(WeatherRegionMigration.shouldMigrate(preferencesOf(LOCATION to "current"))).isFalse()
        assertThat(WeatherRegionMigration.shouldMigrate(preferencesOf(LOCATION to "1168064000"))).isFalse()
        assertThat(WeatherRegionMigration.shouldMigrate(preferencesOf())).isFalse()
    }

    @Test
    fun recentRegions_keepFiveNewestWithoutDuplicates_andCanBeCleared() = scope.runTest {
        listOf("1100000000", "1168000000", "1168064000", "2600000000", "5000000000", "3000000000")
            .forEach { repository.setLocation(WeatherLocation.Region(TestKoreaRegions.area(it))) }
        repository.setLocation(WeatherLocation.Region(TestKoreaRegions.yeoksam1))
        repository.setLocation(WeatherLocation.Current)

        assertThat(repository.recentRegionCodes.first())
            .containsExactly("1168064000", "3000000000", "5000000000", "2600000000", "1168000000")
            .inOrder()

        repository.clearRecentRegions()
        assertThat(repository.recentRegionCodes.first()).isEmpty()
        assertThat(repository.settings.first().location).isEqualTo(WeatherLocation.Current)
    }

    @Test
    fun turningOffOrChangingLocation_forgetsLastWeatherRightAway() = scope.runTest {
        val lastWeather = PreferencesLastWeatherRepository(dataStore)
        val gangnam = WeatherLocation.Region(TestKoreaRegions.yeoksam1)
        val saved = LastWeather(
            snapshot = WeatherSnapshot(WeatherCondition.Clear, 20.0, 1.0, false, Instant.EPOCH),
            fetchedAt = Instant.EPOCH,
            areaName = null,
            locationKey = LastWeatherCache.keyFor(gangnam),
        )
        repository.setLocation(gangnam)
        lastWeather.save(saved)

        // 같은 지역을 다시 고르면 그대로 둔다.
        repository.setLocation(gangnam)
        assertThat(lastWeather.load()).isEqualTo(saved)

        repository.setLocation(WeatherLocation.Region(TestKoreaRegions.busan))
        assertThat(lastWeather.load()).isNull()

        lastWeather.save(saved)
        repository.setEnabled(false)
        assertThat(lastWeather.load()).isNull()
    }

    @Test
    fun dismissingPrompt_isRemembered() = scope.runTest {
        repository.dismissPrompt()

        assertThat(repository.settings.first().isPromptDismissed).isTrue()
    }

    @Test
    fun appUsage_keepsPreviousLaunch() = scope.runTest {
        val usage = PreferencesAppUsageRepository(dataStore)
        val first = Instant.parse("2026-09-01T00:00:00Z")
        val second = Instant.parse("2026-10-01T00:00:00Z")

        usage.recordLaunch(first)
        usage.recordLaunch(second)

        val recorded = usage.usage.first()
        assertThat(recorded.launchCount).isEqualTo(2)
        assertThat(recorded.lastLaunchAt).isEqualTo(second)
        assertThat(recorded.previousLaunchAt).isEqualTo(first)
    }

    private companion object {
        val LOCATION = stringPreferencesKey("weather_location")
    }
}
