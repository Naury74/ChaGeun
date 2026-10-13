package com.naury.chageun.core.domain.time

import com.naury.chageun.core.domain.greeting.GreetingRules
import com.naury.chageun.core.domain.weather.DaylightRules
import com.naury.chageun.core.model.GeoPoint
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * 홈 머리말의 하늘과 인사말을 다시 골라야 하는 다음 시각. 해 뜸·노을 시작·해 짐, 인사말 시간대와 출퇴근 시간이
 * 바뀌는 정각, 날짜가 바뀌는 자정(공휴일·하루 문구) 중 가장 이른 때다. 날씨를 다시 받지 않고 시각만으로 정한다.
 */
object HeaderTimeBoundaries {

    /** [point]는 날씨를 물은 지점이다. 모르면 [DaylightRules.KOREA_CENTER]를 넘긴다. */
    fun next(point: GeoPoint, clock: Clock): Instant = next(point, clock.instant(), clock.zone)

    fun next(point: GeoPoint, now: Instant, zone: ZoneId): Instant = minOf(
        DaylightRules.nextChange(point, now, zone),
        nextGreetingChange(now, zone),
        nextMidnight(now, zone),
    )

    private fun nextGreetingChange(now: Instant, zone: ZoneId): Instant {
        val today = now.atZone(zone).toLocalDate()
        return sequenceOf(today, today.plusDays(1))
            .flatMap { date ->
                GreetingRules.changeHours.asSequence().map { date.atTime(it, 0).atZone(zone).toInstant() }
            }
            .filter { it.isAfter(now) }
            .min()
    }

    private fun nextMidnight(now: Instant, zone: ZoneId): Instant =
        now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
}
