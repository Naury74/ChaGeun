package com.naury.chageun.core.domain.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.KoreaRegion
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSnapshot
import com.naury.chageun.core.testing.FakeApproximateLocationProvider
import com.naury.chageun.core.testing.FakeWeatherRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WeatherTest {

    private fun snapshot(
        condition: WeatherCondition = WeatherCondition.Cloudy,
        temperatureC: Double = 15.0,
        windSpeedMs: Double = 2.0,
        isNight: Boolean = false,
    ) = WeatherSnapshot(condition, temperatureC, windSpeedMs, isNight, Instant.EPOCH)

    @Test
    fun coordinates_roundToOneDecimal() {
        assertThat(GeoPoint(37.56654, 126.97796).roundedForWeather()).isEqualTo(GeoPoint(37.6, 127.0))
        assertThat(GeoPoint(35.14999, 129.05).roundedForWeather()).isEqualTo(GeoPoint(35.1, 129.1))
        assertThat(GeoPoint(-33.86785, 151.20732).roundedForWeather()).isEqualTo(GeoPoint(-33.9, 151.2))
    }

    @Test
    fun tips_followSafetyOrder() {
        val cases = mapOf(
            snapshot(WeatherCondition.Snow, temperatureC = -8.0) to WeatherTip.Snow,
            snapshot(WeatherCondition.Sleet, temperatureC = 1.0) to WeatherTip.Snow,
            snapshot(WeatherCondition.Thunder) to WeatherTip.Thunder,
            snapshot(WeatherCondition.HeavyRain, windSpeedMs = 20.0) to WeatherTip.HeavyRain,
            snapshot(WeatherCondition.Rain, temperatureC = 35.0) to WeatherTip.Rain,
            snapshot(WeatherCondition.Fog) to WeatherTip.Fog,
            snapshot(WeatherCondition.Cloudy, windSpeedMs = 15.0) to WeatherTip.StrongWind,
            snapshot(WeatherCondition.Clear, temperatureC = -9.0) to WeatherTip.Cold,
            snapshot(WeatherCondition.Cloudy, temperatureC = -1.0) to WeatherTip.Freezing,
            snapshot(WeatherCondition.Clear, temperatureC = 34.0) to WeatherTip.Heat,
            snapshot(WeatherCondition.Clear, temperatureC = 22.0) to WeatherTip.NiceDay,
            snapshot(WeatherCondition.Clear, temperatureC = 22.0, isNight = true) to WeatherTip.Calm,
            snapshot(WeatherCondition.Cloudy) to WeatherTip.Calm,
        )

        cases.forEach { (weather, tip) -> assertThat(WeatherTips.tipFor(weather)).isEqualTo(tip) }
    }

    @Test
    fun cache_waitsAtLeastAnHour_orUntilExpires() {
        val fetched = Instant.parse("2026-10-12T00:00:00Z")

        assertThat(WeatherCachePolicy.isFresh(fetched, fetched.plusSeconds(600), fetched.plusSeconds(3_000))).isTrue()
        assertThat(WeatherCachePolicy.isFresh(fetched, null, fetched.plus(Duration.ofMinutes(60)))).isFalse()
        assertThat(
            WeatherCachePolicy.isFresh(fetched, fetched.plus(Duration.ofMinutes(80)), fetched.plusSeconds(4_000)),
        )
            .isTrue()
        assertThat(WeatherCachePolicy.isUsable(fetched, fetched.plus(Duration.ofMinutes(179)))).isTrue()
        assertThat(WeatherCachePolicy.isUsable(fetched, fetched.plus(Duration.ofHours(3)))).isFalse()
    }

    @Test
    fun currentLocation_needsPermission_andSendsOnlyRoundedPoint() = runTest {
        val location = FakeApproximateLocationProvider(permitted = false, location = GeoPoint(37.56654, 126.97796))
        val repository = FakeWeatherRepository(snapshot())
        val load = LoadWeatherUseCase(location, repository)

        assertThat(load(WeatherLocation.Current)).isEqualTo(WeatherResult.NeedsPermission)
        assertThat(repository.requested).isEmpty()

        location.permitted = true
        assertThat(load(WeatherLocation.Current)).isEqualTo(WeatherResult.Shown(snapshot()))
        assertThat(repository.requested).containsExactly(GeoPoint(37.6, 127.0))
    }

    @Test
    fun region_needsNoPermission_andFailureIsUnavailable() = runTest {
        val repository = FakeWeatherRepository(null)
        val load = LoadWeatherUseCase(FakeApproximateLocationProvider(), repository)

        assertThat(load(WeatherLocation.Region(KoreaRegion.Busan))).isEqualTo(WeatherResult.Unavailable)
        assertThat(repository.requested).containsExactly(GeoPoint(35.2, 129.1))

        val noFix = LoadWeatherUseCase(FakeApproximateLocationProvider(permitted = true, location = null), repository)
        assertThat(noFix(WeatherLocation.Current)).isEqualTo(WeatherResult.Unavailable)
    }
}
