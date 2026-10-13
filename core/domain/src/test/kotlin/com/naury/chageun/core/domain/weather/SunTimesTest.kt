package com.naury.chageun.core.domain.weather

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.naury.chageun.core.model.GeoPoint
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class SunTimesTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    // 한국천문연구원 일출몰시각 계산(서울)과 비교한다. 날짜, 해 뜨는 시각, 해 지는 시각.
    private val seoulCases = listOf(
        Triple(LocalDate.of(2026, 6, 21), LocalTime.of(5, 11), LocalTime.of(19, 57)),
        Triple(LocalDate.of(2026, 12, 22), LocalTime.of(7, 43), LocalTime.of(17, 17)),
        Triple(LocalDate.of(2026, 3, 20), LocalTime.of(6, 36), LocalTime.of(18, 44)),
        Triple(LocalDate.of(2026, 10, 12), LocalTime.of(6, 37), LocalTime.of(18, 0)),
    )

    @Test
    fun seoul_matchesPublishedTimes_withinTwoMinutes() {
        seoulCases.forEach { (date, sunrise, sunset) ->
            val sun = checkNotNull(SolarCalculator.sunTimes(SEOUL, date))
            assertWithMessage("$date sunrise ${sun.sunrise.atZone(seoul)}")
                .that(minutesBetween(sun.sunrise.atZone(seoul), sunrise)).isAtMost(2)
            assertWithMessage("$date sunset ${sun.sunset.atZone(seoul)}")
                .that(minutesBetween(sun.sunset.atZone(seoul), sunset)).isAtMost(2)
        }
    }

    @Test
    fun polarNight_hasNoSunTimes() {
        assertThat(SolarCalculator.sunTimes(GeoPoint(78.2, 15.6), LocalDate.of(2026, 12, 21))).isNull()
    }

    @Test
    fun daylight_followsSunriseAndSunset_andDuskIsHalfHourBeforeSunset() {
        val day = LocalDate.of(2026, 10, 12)
        fun at(hour: Int, minute: Int, providerSaysNight: Boolean = false) = DaylightRules.at(
            SEOUL,
            ZonedDateTime.of(day, LocalTime.of(hour, minute), seoul).toInstant(),
            seoul,
            providerSaysNight,
        )

        assertThat(at(6, 30)).isEqualTo(Daylight.Night)
        assertThat(at(6, 45)).isEqualTo(Daylight.Day)
        assertThat(at(17, 20)).isEqualTo(Daylight.Day)
        assertThat(at(17, 40)).isEqualTo(Daylight.Dusk)
        // 예전에는 18시 전이면 낮으로 봤지만, 10월에는 18시 무렵 이미 해가 진다.
        assertThat(at(18, 5)).isEqualTo(Daylight.Night)
        // 제공처가 밤 아이콘을 주면 그대로 밤이다.
        assertThat(at(12, 0, providerSaysNight = true)).isEqualTo(Daylight.Night)
    }

    @Test
    fun providerNightIcon_isDroppedOnceTheSunRisesAfterIt() {
        val day = LocalDate.of(2026, 10, 12)
        fun instant(hour: Int, minute: Int) = ZonedDateTime.of(day, LocalTime.of(hour, minute), seoul).toInstant()
        fun at(hour: Int, minute: Int) =
            DaylightRules.at(SEOUL, instant(hour, minute), seoul, providerSaysNight = true, observedAt = instant(5, 0))

        // 새벽 5시에 받은 밤 날씨는 해가 뜨기 전까지 밤이다.
        assertThat(at(6, 30)).isEqualTo(Daylight.Night)
        // 홈을 켜 둔 채 해가 뜨면 다시 받지 않아도 낮이다.
        assertThat(at(6, 45)).isEqualTo(Daylight.Day)
        assertThat(at(17, 40)).isEqualTo(Daylight.Dusk)
    }

    @Test
    fun nextChange_isTheNextSunriseDuskOrSunset() {
        val day = LocalDate.of(2026, 10, 12)
        val sun = checkNotNull(SolarCalculator.sunTimes(SEOUL, day))
        fun next(hour: Int) =
            DaylightRules.nextChange(SEOUL, ZonedDateTime.of(day, LocalTime.of(hour, 0), seoul).toInstant(), seoul)

        assertThat(next(3)).isEqualTo(sun.sunrise)
        assertThat(next(12)).isEqualTo(sun.sunset.minus(DaylightRules.DUSK))
        assertThat(next(21)).isEqualTo(checkNotNull(SolarCalculator.sunTimes(SEOUL, day.plusDays(1))).sunrise)
    }

    @Test
    fun summerEvening_isStillDayAfterSix() {
        val sevenPm = ZonedDateTime.of(LocalDate.of(2026, 6, 21), LocalTime.of(19, 0), seoul).toInstant()

        assertThat(DaylightRules.at(SEOUL, sevenPm, seoul)).isEqualTo(Daylight.Day)
    }

    private fun minutesBetween(actual: ZonedDateTime, expected: LocalTime): Long =
        Duration.between(actual.toLocalTime(), expected).abs().toMinutes()

    private companion object {
        // 한국천문연구원 서울 기준점(서울특별시청 근처).
        val SEOUL = GeoPoint(37.5665, 126.978)
    }
}
