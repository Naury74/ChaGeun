package com.naury.chageun.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.KoreaRegion
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
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
    private val repository = PreferencesWeatherSettingsRepository(dataStore)

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
        repository.setLocation(WeatherLocation.Region(KoreaRegion.Jeju))
        assertThat(repository.settings.first())
            .isEqualTo(WeatherSettings(isEnabled = true, location = WeatherLocation.Region(KoreaRegion.Jeju)))

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
        repository.setLocation(WeatherLocation.Region(KoreaRegion.Busan))
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
        repository.setLocation(WeatherLocation.Region(KoreaRegion.Seoul))
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
        repository.setLocation(WeatherLocation.Region(KoreaRegion.Jeju))
        assertThat(repository.isCurrentLocationMissing.first()).isFalse()
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
}
