package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.GeoPoint
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** 한 지점의 하루 해 뜨는 시각과 지는 시각. */
data class SunTimes(val sunrise: Instant, val sunset: Instant)

/**
 * NOAA 태양 위치 계산식으로 해 뜨고 지는 시각을 구한다. 네트워크 없이 기기에서 계산하고, 오차는 1~2분이다.
 * 해가 지평선 아래 0.833도(대기 굴절과 해의 반지름)에 걸칠 때를 뜨고 지는 순간으로 본다.
 */
object SolarCalculator {

    /** [date]의 해 뜨고 지는 시각. 백야·극야처럼 해가 뜨거나 지지 않는 날이면 null이다. */
    fun sunTimes(point: GeoPoint, date: LocalDate): SunTimes? {
        val midnightUtc = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val sunrise = eventMinutes(point, date, rising = true) ?: return null
        val sunset = eventMinutes(point, date, rising = false) ?: return null
        return SunTimes(midnightUtc.plusMinutes(sunrise), midnightUtc.plusMinutes(sunset))
    }

    // 처음엔 남중 무렵의 해 위치로 계산하고, 구한 시각의 해 위치로 두 번 더 고쳐 정확도를 높인다.
    private fun eventMinutes(point: GeoPoint, date: LocalDate, rising: Boolean): Double? {
        val julianDayAtMidnight = date.toEpochDay() + JULIAN_DAY_AT_EPOCH
        var minutes = SOLAR_NOON_MINUTES - MINUTES_PER_DEGREE * point.longitude
        repeat(ITERATIONS) {
            val sun = SunPosition.at(julianDayAtMidnight + minutes / MINUTES_PER_DAY)
            val hourAngle = sun.hourAngle(point.latitude) ?: return null
            val noon = SOLAR_NOON_MINUTES - MINUTES_PER_DEGREE * point.longitude - sun.equationOfTimeMinutes
            minutes = noon + MINUTES_PER_DEGREE * if (rising) -hourAngle else hourAngle
        }
        return minutes
    }

    private fun Instant.plusMinutes(minutes: Double): Instant =
        plus(Duration.ofMillis((minutes * MILLIS_PER_MINUTE).toLong()))

    private const val JULIAN_DAY_AT_EPOCH = 2_440_587.5
    private const val SOLAR_NOON_MINUTES = 720.0
    private const val MINUTES_PER_DEGREE = 4.0
    private const val MINUTES_PER_DAY = 1_440.0
    private const val MILLIS_PER_MINUTE = 60_000.0
    private const val ITERATIONS = 3
}

/** 하늘을 그리는 밝기. 해 지기 전 30분은 노을([Dusk])로 본다. */
enum class Daylight { Day, Dusk, Night }

object DaylightRules {
    /** 노을빛을 보이는 시간. 해 지기 바로 전이다. */
    val DUSK: Duration = Duration.ofMinutes(30)

    /**
     * 위치를 모를 때 쓰는 국토 가운데쯤(충북 괴산 근처). 저장해 둔 날씨는 좌표 없이 두므로 이 값으로 계산하고,
     * 국내 어디든 해 지는 시각과 차이는 대개 20분 안이다.
     */
    val KOREA_CENTER: GeoPoint = GeoPoint(36.5, 127.8)

    /**
     * [now]가 [point]에서 낮·노을·밤 중 어디인지. 날씨 제공처가 밤 아이콘을 줬으면([providerSaysNight]) 그대로 밤이다.
     * 다만 그 날씨의 시각([observedAt]) 뒤로 해가 뜨거나 지는 때를 지났으면 아이콘이 낡은 것이라 해 위치로만 정한다.
     * 해가 뜨거나 지지 않는 날은 기기 시각으로 6시~18시를 낮으로 본다.
     */
    fun at(
        point: GeoPoint,
        now: Instant,
        zone: ZoneId,
        providerSaysNight: Boolean = false,
        observedAt: Instant? = null,
    ): Daylight {
        val isProviderNightCurrent = observedAt == null || now.isBefore(nextChange(point, observedAt, zone))
        if (providerSaysNight && isProviderNightCurrent) return Daylight.Night
        val sun = SolarCalculator.sunTimes(point, now.atZone(zone).toLocalDate())
            ?: return if (now.atZone(zone).hour in FALLBACK_DAY_HOURS) Daylight.Day else Daylight.Night
        return when {
            now.isBefore(sun.sunrise) || !now.isBefore(sun.sunset) -> Daylight.Night
            !now.isBefore(sun.sunset.minus(DUSK)) -> Daylight.Dusk
            else -> Daylight.Day
        }
    }

    /**
     * [now] 다음으로 낮·노을·밤이 바뀔 수 있는 시각. 해 뜸, 노을 시작, 해 짐 중 가장 이른 때다.
     * 해가 뜨거나 지지 않는 날은 기기 시각 6시와 18시다. 앞뒤 날짜까지 같이 봐서 시간대가 경도와 어긋나도 빠뜨리지 않는다.
     */
    fun nextChange(point: GeoPoint, now: Instant, zone: ZoneId): Instant {
        val today = now.atZone(zone).toLocalDate()
        return (-1L..2L).asSequence()
            .flatMap { changesOn(point, today.plusDays(it), zone) }
            .filter { it.isAfter(now) }
            .min()
    }

    private fun changesOn(point: GeoPoint, date: LocalDate, zone: ZoneId): Sequence<Instant> {
        val sun = SolarCalculator.sunTimes(point, date)
            ?: return FALLBACK_CHANGE_HOURS.asSequence().map { date.atTime(it, 0).atZone(zone).toInstant() }
        return sequenceOf(sun.sunrise, sun.sunset.minus(DUSK), sun.sunset)
    }

    private val FALLBACK_DAY_HOURS = 6 until 18
    private val FALLBACK_CHANGE_HOURS = listOf(FALLBACK_DAY_HOURS.first, FALLBACK_DAY_HOURS.last + 1)
}

/** 한 순간의 해 위치 중 해 뜨고 지는 시각에 필요한 값. 각도는 도 단위다. */
private class SunPosition(val declination: Double, val equationOfTimeMinutes: Double) {

    /** 해가 지평선에 걸칠 때의 시간각. 그날 해가 뜨거나 지지 않으면 null이다. */
    fun hourAngle(latitude: Double): Double? {
        val lat = Math.toRadians(latitude)
        val dec = Math.toRadians(declination)
        val cosHourAngle = cos(Math.toRadians(ZENITH)) / (cos(lat) * cos(dec)) - tan(lat) * tan(dec)
        return if (cosHourAngle in -1.0..1.0) Math.toDegrees(acos(cosHourAngle)) else null
    }

    // NOAA Solar Calculator 스프레드시트의 식을 그대로 옮겼다. 계수는 그 식의 값이다.
    companion object {
        private const val ZENITH = 90.833
        private const val J2000 = 2_451_545.0
        private const val DAYS_PER_CENTURY = 36_525.0
        private const val FULL_CIRCLE = 360.0
        private const val ARC_MINUTES = 60.0
        private const val OBLIQUITY_DEGREES = 23.0
        private const val OBLIQUITY_MINUTES = 26.0
        private const val MINUTES_PER_DEGREE = 4.0
        private const val THIRD = 3.0
        private const val FOURTH = 4.0
        private const val HALF = 0.5
        private const val ECCENTRICITY_SQUARED_FACTOR = 1.25

        fun at(julianDay: Double): SunPosition {
            val t = (julianDay - J2000) / DAYS_PER_CENTURY
            val l0 = poly(t, c0 = 280.46646, c1 = 36_000.76983, c2 = 0.0003032).mod(FULL_CIRCLE)
            val m = poly(t, c0 = 357.52911, c1 = 35_999.05029, c2 = -0.0001537)
            val e = poly(t, c0 = 0.016708634, c1 = -0.000042037, c2 = -0.0000001267)
            val center = sinDeg(m) * poly(t, c0 = 1.914602, c1 = -0.004817, c2 = -0.000014) +
                sinDeg(2 * m) * poly(t, c0 = 0.019993, c1 = -0.000101) + sinDeg(THIRD * m) * poly(t, c0 = 0.000289)
            val omega = poly(t, c0 = 125.04, c1 = -1_934.136)
            val apparent = l0 + center + poly(sinDeg(omega), c0 = -0.00569, c1 = -0.00478)
            val seconds = poly(t, c0 = 21.448, c1 = -46.815, c2 = -0.00059, c3 = 0.001813)
            val meanObliquity = OBLIQUITY_DEGREES + (OBLIQUITY_MINUTES + seconds / ARC_MINUTES) / ARC_MINUTES
            val obliquity = meanObliquity + poly(cos(Math.toRadians(omega)), c0 = 0.0, c1 = 0.00256)
            val declination = Math.toDegrees(asin(sinDeg(obliquity) * sinDeg(apparent)))
            val y = tan(Math.toRadians(obliquity) / 2).let { it * it }
            val equationOfTime = MINUTES_PER_DEGREE * Math.toDegrees(
                y * sinDeg(2 * l0) - 2 * e * sinDeg(m) + FOURTH * e * y * sinDeg(m) * cosDeg(2 * l0) -
                    HALF * y * y * sinDeg(FOURTH * l0) - ECCENTRICITY_SQUARED_FACTOR * e * e * sinDeg(2 * m),
            )
            return SunPosition(declination, equationOfTime)
        }

        private fun poly(t: Double, c0: Double, c1: Double = 0.0, c2: Double = 0.0, c3: Double = 0.0): Double =
            c0 + t * (c1 + t * (c2 + t * c3))

        private fun sinDeg(degrees: Double): Double = sin(Math.toRadians(degrees))

        private fun cosDeg(degrees: Double): Double = cos(Math.toRadians(degrees))
    }
}
