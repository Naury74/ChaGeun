package com.naury.chageun.data.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.model.GeoPoint
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GeocoderAreaNameRepositoryTest {

    private val clock = MutableClock(Instant.parse("2026-10-12T05:40:00Z"))
    private val geocoder = FakeGeocoder()
    private val logged = mutableListOf<String>()
    private val logger = object : AppLogger {
        override fun debug(event: String, vararg fields: LogField) = record(event, fields)

        override fun warn(event: String, vararg fields: LogField, error: Throwable?) = record(event, fields)

        override fun error(event: String, vararg fields: LogField, error: Throwable?) = record(event, fields)

        private fun record(event: String, fields: Array<out LogField>) {
            logged += event
            logged += fields.map { it.value }
        }
    }
    private val repository = GeocoderAreaNameRepository(geocoder, clock, logger).apply {
        currentLocale = { Locale.KOREAN }
    }
    private val gangnam = AreaAddress("KR", adminArea = "서울특별시", subLocality = "강남구")

    @Test
    fun looksUpRoundedPointOnly_andReusesWhileRoundedPointIsSame() = runTest {
        geocoder.next = gangnam

        assertThat(repository.areaNameAt(GeoPoint(37.49794, 127.02762))).isEqualTo("서울 강남구")
        // 반올림하면 같은 칸이라 다시 묻지 않는다.
        assertThat(repository.areaNameAt(GeoPoint(37.51, 127.04))).isEqualTo("서울 강남구")

        assertThat(geocoder.calls).containsExactly(GeoPoint(37.5, 127.0) to Locale.KOREAN)
    }

    @Test
    fun newRoundedPointOrLanguage_looksUpAgain() = runTest {
        geocoder.next = gangnam
        repository.areaNameAt(GeoPoint(37.5, 127.0))

        geocoder.next = AreaAddress("KR", adminArea = "경기도", locality = "성남시", subLocality = "분당구")
        assertThat(repository.areaNameAt(GeoPoint(37.4, 127.1))).isEqualTo("경기 성남시")

        repository.currentLocale = { Locale.ENGLISH }
        geocoder.next = AreaAddress("KR", adminArea = "Gyeonggi-do", locality = "Seongnam-si")
        assertThat(repository.areaNameAt(GeoPoint(37.4, 127.1))).isEqualTo("Seongnam-si, Gyeonggi")

        assertThat(geocoder.calls).hasSize(3)
    }

    @Test
    fun failure_isNull_andRetriesOnlyAfterAWhile() = runTest {
        geocoder.next = null

        assertThat(repository.areaNameAt(GeoPoint(37.5, 127.0))).isNull()
        clock.advance(Duration.ofMinutes(10))
        assertThat(repository.areaNameAt(GeoPoint(37.5, 127.0))).isNull()
        assertThat(geocoder.calls).hasSize(1)

        geocoder.next = gangnam
        clock.advance(GeocoderAreaNameRepository.RETRY_AFTER)
        assertThat(repository.areaNameAt(GeoPoint(37.5, 127.0))).isEqualTo("서울 강남구")
        assertThat(geocoder.calls).hasSize(2)
    }

    @Test
    fun neverLogsCoordinatesOrAreaName() = runTest {
        geocoder.next = null
        repository.areaNameAt(GeoPoint(37.5, 127.0))
        geocoder.next = gangnam
        repository.areaNameAt(GeoPoint(35.2, 129.1))

        assertThat(logged).containsExactly("area_name_unavailable")
        logged.forEach {
            assertThat(it).doesNotContain("37.5")
            assertThat(it).doesNotContain("강남")
        }
    }

    private class FakeGeocoder : AreaGeocoder {
        var next: AreaAddress? = null
        val calls = mutableListOf<Pair<GeoPoint, Locale>>()

        override suspend fun lookup(point: GeoPoint, locale: Locale): AreaAddress? {
            calls += point to locale
            return next
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
