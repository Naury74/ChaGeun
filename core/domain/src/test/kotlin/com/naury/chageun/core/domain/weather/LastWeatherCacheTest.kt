package com.naury.chageun.core.domain.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSnapshot
import com.naury.chageun.core.testing.FakeLastWeatherRepository
import com.naury.chageun.core.testing.TestKoreaRegions
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LastWeatherCacheTest {

    private val repository = FakeLastWeatherRepository()
    private val cache = LastWeatherCache(repository)
    private val now = Instant.parse("2026-10-12T09:00:00Z")
    private val snapshot = WeatherSnapshot(WeatherCondition.Clear, 21.0, 2.0, false, now)

    @Test
    fun savedWeather_isUsableForThreeHours_thenDropped() = runTest {
        cache.remember(WeatherLocation.Current, snapshot, "서울 강남구", now)

        val saved = cache.usable(WeatherLocation.Current, now.plus(Duration.ofMinutes(179)))
        assertThat(saved?.snapshot).isEqualTo(snapshot)
        assertThat(saved?.areaName).isEqualTo("서울 강남구")

        assertThat(cache.usable(WeatherLocation.Current, now.plus(Duration.ofHours(3)))).isNull()
        assertThat(repository.saved).isNull()
    }

    @Test
    fun otherLocation_isNotShown_andIsCleared() = runTest {
        cache.remember(WeatherLocation.Region(TestKoreaRegions.seoul), snapshot, null, now)

        assertThat(cache.usable(WeatherLocation.Region(TestKoreaRegions.busan), now)).isNull()
        assertThat(repository.saved).isNull()
    }

    @Test
    fun sameWeatherAgain_keepsFirstFetchTime() = runTest {
        cache.remember(WeatherLocation.Current, snapshot, null, now)
        // 저장소가 메모리 캐시에서 같은 날씨를 돌려줘도 받은 시각을 늦추지 않는다.
        cache.remember(WeatherLocation.Current, snapshot, null, now.plus(Duration.ofHours(2)))

        assertThat(repository.saveCount).isEqualTo(1)
        assertThat(repository.saved?.fetchedAt).isEqualTo(now)

        val warmer = snapshot.copy(temperatureC = 23.0)
        cache.remember(WeatherLocation.Current, warmer, null, now.plus(Duration.ofHours(2)))
        assertThat(repository.saved?.fetchedAt).isEqualTo(now.plus(Duration.ofHours(2)))

        cache.clear()
        assertThat(repository.saved).isNull()
    }

    @Test
    fun key_isTheRegionCode_notItsNamesOrPoint() {
        val area = TestKoreaRegions.yeoksam1
        val renamed = WeatherLocation.Region(area.copy(label = "역삼1동", point = GeoPoint(0.0, 0.0)))

        assertThat(LastWeatherCache.keyFor(WeatherLocation.Region(area))).isEqualTo("region:1168064000")
        assertThat(LastWeatherCache.keyFor(renamed)).isEqualTo("region:1168064000")
        assertThat(LastWeatherCache.keyFor(WeatherLocation.Current)).isEqualTo("current")
    }
}
