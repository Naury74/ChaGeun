package com.naury.chageun.data.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.model.GeoPoint
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MetNorwayWeatherRepositoryTest {

    private val body = checkNotNull(javaClass.classLoader?.getResource("met_compact_seoul.json")).readText()
    private val clock = MutableClock(Instant.parse("2026-10-12T05:40:00Z"))
    private val remote = FakeRemote()
    private val loggedEvents = mutableListOf<String>()
    private val logger = object : AppLogger {
        override fun debug(event: String, vararg fields: LogField) = Unit

        override fun warn(event: String, vararg fields: LogField, error: Throwable?) {
            loggedEvents += event
            // 좌표는 어떤 로그 값에도 들어가면 안 된다.
            fields.forEach { assertThat(it.value).doesNotContain("37.6") }
        }

        override fun error(event: String, vararg fields: LogField, error: Throwable?) = Unit
    }
    private val repository = MetNorwayWeatherRepository(remote, clock, logger)
    private val seoul = GeoPoint(37.6, 127.0)

    @Test
    fun reusesForecast_untilAnHourPassed_evenIfExpiresIsEarlier() = runTest {
        remote.next = { MetFetch.Fresh(body, expires = clock.instant().plusSeconds(1_800), lastModified = "LM1") }

        repository.weatherAt(seoul)
        clock.advance(Duration.ofMinutes(59))
        repository.weatherAt(seoul)

        assertThat(remote.calls).hasSize(1)

        clock.advance(Duration.ofMinutes(2))
        remote.next = { MetFetch.NotModified(expires = null) }
        val refreshed = repository.weatherAt(seoul)

        assertThat(remote.calls).hasSize(2)
        // 두 번째 요청은 받은 Last-Modified로 바뀐 것만 묻는다.
        assertThat(remote.calls.last().second).isEqualTo("LM1")
        // 304여도 지금 시각에 맞는 칸을 다시 고른다(06:41 → 06시 칸, 강한 비).
        assertThat(refreshed?.temperatureC).isEqualTo(18.4)
    }

    @Test
    fun honorsLaterExpires() = runTest {
        remote.next = { MetFetch.Fresh(body, expires = clock.instant().plus(Duration.ofMinutes(90)), null) }

        repository.weatherAt(seoul)
        clock.advance(Duration.ofMinutes(75))
        repository.weatherAt(seoul)

        assertThat(remote.calls).hasSize(1)
    }

    @Test
    fun anotherPoint_fetchesAgain() = runTest {
        remote.next = { MetFetch.Fresh(body, null, null) }

        repository.weatherAt(seoul)
        repository.weatherAt(GeoPoint(35.2, 129.1))

        assertThat(remote.calls.map { it.first }).containsExactly(seoul, GeoPoint(35.2, 129.1)).inOrder()
    }

    @Test
    fun failure_keepsShowingRecentForecast_thenGivesUp() = runTest {
        remote.next = { MetFetch.Fresh(body, null, null) }
        repository.weatherAt(seoul)

        remote.next = { throw IOException("offline") }
        clock.advance(Duration.ofMinutes(90))
        assertThat(repository.weatherAt(seoul)).isNotNull()

        clock.advance(Duration.ofHours(2))
        assertThat(repository.weatherAt(seoul)).isNull()
        assertThat(loggedEvents).contains("weather_fetch_failed")
    }

    @Test
    fun failureWithoutCache_isNull() = runTest {
        remote.next = { throw IOException("offline") }

        assertThat(repository.weatherAt(seoul)).isNull()
    }

    @Test
    fun unreadableBody_isNull() = runTest {
        remote.next = { MetFetch.Fresh("<html>", null, null) }

        assertThat(repository.weatherAt(seoul)).isNull()
        assertThat(loggedEvents).contains("weather_parse_failed")
    }

    private class FakeRemote : MetNorwayRemote {
        var next: () -> MetFetch = { throw IOException("unset") }
        val calls = mutableListOf<Pair<GeoPoint, String?>>()

        override suspend fun fetch(point: GeoPoint, ifModifiedSince: String?): MetFetch {
            calls += point to ifModifiedSince
            return next()
        }
    }

    private class MutableClock(private var now: Instant) : Clock() {
        fun advance(duration: Duration) {
            now = now.plus(duration)
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this
    }
}
