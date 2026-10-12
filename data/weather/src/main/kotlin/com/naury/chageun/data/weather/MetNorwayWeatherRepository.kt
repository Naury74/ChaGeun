package com.naury.chageun.data.weather

import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.domain.weather.WeatherCachePolicy
import com.naury.chageun.core.domain.weather.WeatherRepository
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherSnapshot
import java.io.IOException
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 마지막으로 받은 날씨 하나를 메모리에만 둔다. 앱을 다시 켜면 처음부터 받는다.
 * 좌표·응답 본문은 디스크와 로그에 남기지 않는다.
 */
@Singleton
internal class MetNorwayWeatherRepository @Inject constructor(
    private val remote: MetNorwayRemote,
    private val clock: Clock,
    private val logger: AppLogger,
) : WeatherRepository {

    private val mutex = Mutex()
    private var cached: CachedForecast? = null

    override suspend fun weatherAt(point: GeoPoint): WeatherSnapshot? = mutex.withLock {
        val now = clock.instant()
        val hit = cached?.takeIf { it.point == point }
        if (hit != null && WeatherCachePolicy.isFresh(hit.fetchedAt, hit.expires, now)) {
            return@withLock hit.snapshot
        }
        val refreshed = refresh(point, hit, now)
        if (refreshed != null) cached = refreshed
        (refreshed ?: hit?.takeIf { WeatherCachePolicy.isUsable(it.fetchedAt, now) })?.snapshot
    }

    private suspend fun refresh(point: GeoPoint, previous: CachedForecast?, now: Instant): CachedForecast? {
        val fetched = fetchOrNull(point, previous?.lastModified) ?: return null
        val response = when (fetched) {
            is MetFetch.Fresh -> MetFetch.Fresh(fetched.body, fetched.expires, fetched.lastModified)
            // 304는 가진 응답이 있을 때만 온다. 같은 본문에서 지금 시각에 맞는 칸을 다시 고른다.
            is MetFetch.NotModified -> previous?.let { MetFetch.Fresh(it.body, fetched.expires, it.lastModified) }
        } ?: return null
        val snapshot = MetForecastParser.parse(response.body, now)
        if (snapshot == null) logger.warn("weather_parse_failed")
        return snapshot?.let {
            CachedForecast(point, response.body, it, now, response.expires, response.lastModified)
        }
    }

    private suspend fun fetchOrNull(point: GeoPoint, lastModified: String?): MetFetch? = try {
        remote.fetch(point, lastModified)
    } catch (e: IOException) {
        logger.warn("weather_fetch_failed", LogField.ErrorType(e::class.java.simpleName))
        null
    }

    private data class CachedForecast(
        val point: GeoPoint,
        val body: String,
        val snapshot: WeatherSnapshot,
        val fetchedAt: Instant,
        val expires: Instant?,
        val lastModified: String?,
    )
}
