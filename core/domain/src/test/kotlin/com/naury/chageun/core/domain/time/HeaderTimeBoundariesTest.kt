package com.naury.chageun.core.domain.time

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.greeting.GreetingRules
import com.naury.chageun.core.domain.weather.DaylightRules
import com.naury.chageun.core.domain.weather.SolarCalculator
import com.naury.chageun.core.model.GeoPoint
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class HeaderTimeBoundariesTest {

    private val seoulZone = ZoneId.of("Asia/Seoul")

    // 2026년 10월 20일 서울은 6시 44분쯤 해가 뜨고 17시 50분쯤 진다. 노을은 17시 20분쯤 시작한다.
    private val day = LocalDate.of(2026, 10, 20)
    private val sun = checkNotNull(SolarCalculator.sunTimes(SEOUL, day))

    @Test
    fun beforeSunrise_wakesAtSunrise() {
        assertThat(sun.sunrise.atZone(seoulZone).hour).isEqualTo(6)

        assertThat(nextAt(6, 0)).isEqualTo(sun.sunrise)
    }

    @Test
    fun aroundSunset_wakesAtDuskThenSunsetThenTheEveningBand() {
        val dusk = sun.sunset.minus(DaylightRules.DUSK)

        assertThat(nextAt(17, 10)).isEqualTo(dusk)
        assertThat(nextAt(dusk.plusSeconds(60))).isEqualTo(sun.sunset)
        // 해가 진 뒤에는 18시 저녁 인사가 다음이다.
        assertThat(nextAt(sun.sunset.plusSeconds(60))).isEqualTo(local(day, 18, 0))
    }

    @Test
    fun greetingBandsAndCommuteHours_areBoundaries() {
        assertThat(nextAt(8, 15)).isEqualTo(local(day, 9, 0))
        assertThat(nextAt(10, 30)).isEqualTo(local(day, 11, 0))
        assertThat(nextAt(14, 0)).isEqualTo(local(day, 15, 0))
        assertThat(nextAt(16, 10)).isEqualTo(local(day, 17, 0))
        assertThat(nextAt(21, 10)).isEqualTo(local(day, 22, 0))
    }

    @Test
    fun lateNight_wakesAtMidnight_forTheNewDaysGreeting() {
        assertThat(nextAt(23, 30)).isEqualTo(local(day.plusDays(1), 0, 0))
    }

    @Test
    fun polarNight_fallsBackToSixAndEighteen() {
        val svalbard = GeoPoint(78.2, 15.6)
        val zone = ZoneId.of("Arctic/Longyearbyen")
        val winter = LocalDate.of(2026, 12, 21)
        assertThat(SolarCalculator.sunTimes(svalbard, winter)).isNull()

        val dawn = ZonedDateTime.of(winter, LocalTime.of(5, 30), zone).toInstant()

        assertThat(HeaderTimeBoundaries.next(svalbard, dawn, zone))
            .isEqualTo(ZonedDateTime.of(winter, LocalTime.of(6, 0), zone).toInstant())
    }

    @Test
    fun fixedClock_givesTheSameAnswer() {
        val now = local(day, 10, 30)

        assertThat(HeaderTimeBoundaries.next(SEOUL, Clock.fixed(now, seoulZone)))
            .isEqualTo(HeaderTimeBoundaries.next(SEOUL, now, seoulZone))
    }

    @Test
    fun greetingChangeHours_coverEveryTimeRule() {
        assertThat(GreetingRules.changeHours).containsExactly(0, 5, 7, 8, 9, 11, 12, 13, 15, 17, 18, 20, 22)
    }

    private fun nextAt(hour: Int, minute: Int): Instant = nextAt(local(day, hour, minute))

    private fun nextAt(now: Instant): Instant = HeaderTimeBoundaries.next(SEOUL, now, seoulZone)

    private fun local(date: LocalDate, hour: Int, minute: Int): Instant =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), seoulZone).toInstant()

    private companion object {
        val SEOUL = GeoPoint(37.5665, 126.978)
    }
}
