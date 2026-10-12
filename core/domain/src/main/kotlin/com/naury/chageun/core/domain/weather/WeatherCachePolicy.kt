package com.naury.chageun.core.domain.weather

import java.time.Duration
import java.time.Instant

/**
 * 날씨를 다시 받을 때를 정한다. 제공처(MET Norway)가 자주 묻지 말라고 하므로 한 번 받으면 적어도 [MIN_REFRESH] 동안은
 * 그대로 쓴다. 응답의 Expires가 그보다 늦으면 그때까지 기다린다.
 * 새로 받지 못했을 때는 [MAX_AGE]까지 지난 값을 보여 주고, 그보다 오래되면 날씨를 숨긴다.
 */
object WeatherCachePolicy {
    val MIN_REFRESH: Duration = Duration.ofMinutes(60)
    val MAX_AGE: Duration = Duration.ofHours(3)

    fun nextRefreshAt(fetchedAt: Instant, expires: Instant?): Instant {
        val earliest = fetchedAt.plus(MIN_REFRESH)
        return if (expires != null && expires.isAfter(earliest)) expires else earliest
    }

    fun isFresh(fetchedAt: Instant, expires: Instant?, now: Instant): Boolean =
        now.isBefore(nextRefreshAt(fetchedAt, expires))

    fun isUsable(fetchedAt: Instant, now: Instant): Boolean = now.isBefore(fetchedAt.plus(MAX_AGE))
}
