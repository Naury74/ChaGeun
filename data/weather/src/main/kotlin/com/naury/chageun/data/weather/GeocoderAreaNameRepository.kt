package com.naury.chageun.data.weather

import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.domain.weather.AreaNameRepository
import com.naury.chageun.core.model.GeoPoint
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 마지막으로 찾은 동네 이름 하나를 메모리에 둔다. 반올림한 좌표나 앱 언어가 바뀔 때만 다시 찾는다.
 * 못 찾았으면 [RETRY_AFTER] 동안은 다시 묻지 않는다. 좌표와 동네 이름은 로그에 남기지 않는다.
 */
@Singleton
internal class GeocoderAreaNameRepository @Inject constructor(
    private val geocoder: AreaGeocoder,
    private val clock: Clock,
    private val logger: AppLogger,
) : AreaNameRepository {

    /** 테스트에서 언어를 바꿔 본다. */
    internal var currentLocale: () -> Locale = Locale::getDefault

    private val mutex = Mutex()
    private var cached: CachedArea? = null

    override suspend fun areaNameAt(point: GeoPoint): String? = mutex.withLock {
        // 날씨와 같은 반올림 좌표만 묻는다. 정밀한 좌표가 넘어와도 그보다 자세히 찾지 않는다.
        val rounded = point.roundedForWeather()
        val locale = currentLocale()
        val now = clock.instant()
        val hit = cached?.takeIf { it.point == rounded && it.locale == locale }
        if (hit != null && (hit.name != null || now.isBefore(hit.lookedUpAt.plus(RETRY_AFTER)))) {
            return@withLock hit.name
        }
        val name = geocoder.lookup(rounded, locale)?.let { AreaLabels.label(it, locale) }
        if (name == null) logger.debug("area_name_unavailable")
        cached = CachedArea(rounded, locale, name, now)
        name
    }

    private data class CachedArea(val point: GeoPoint, val locale: Locale, val name: String?, val lookedUpAt: Instant)

    internal companion object {
        val RETRY_AFTER: Duration = Duration.ofMinutes(30)
    }
}
