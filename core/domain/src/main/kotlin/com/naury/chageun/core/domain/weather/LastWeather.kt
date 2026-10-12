package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSnapshot
import java.time.Instant
import javax.inject.Inject

/**
 * 마지막으로 받은 날씨. 다음에 앱을 켰을 때 새로 받는 동안 바로 보여 준다.
 * 좌표는 담지 않고, 어느 위치 설정으로 받았는지([locationKey])와 동네 이름만 둔다.
 */
data class LastWeather(
    val snapshot: WeatherSnapshot,
    val fetchedAt: Instant,
    val areaName: String?,
    val locationKey: String,
)

/** [LastWeather] 하나를 기기에 둔다. 날씨를 끄거나 모든 데이터를 지우면 비운다. */
interface LastWeatherRepository {
    suspend fun load(): LastWeather?

    suspend fun save(weather: LastWeather)

    suspend fun clear()
}

/**
 * 저장해 둔 날씨를 언제 꺼내고 언제 새로 쓸지 정한다.
 * 받은 지 [WeatherCachePolicy.MAX_AGE]가 지났거나 다른 위치로 받은 날씨는 보여 주지 않고 지운다.
 */
class LastWeatherCache @Inject constructor(private val repository: LastWeatherRepository) {

    /** [location]으로 받은, 아직 쓸 만한 날씨. 없으면 null이다. */
    suspend fun usable(location: WeatherLocation, now: Instant): LastWeather? {
        val saved = repository.load() ?: return null
        val matches = saved.locationKey == location.cacheKey() && WeatherCachePolicy.isUsable(saved.fetchedAt, now)
        if (!matches) repository.clear()
        return saved.takeIf { matches }
    }

    /**
     * 새로 보여 준 날씨를 저장한다. 저장소가 메모리 캐시에서 같은 날씨를 다시 돌려준 경우에는
     * 받은 시각이 늘어나지 않도록 그대로 둔다.
     */
    suspend fun remember(location: WeatherLocation, snapshot: WeatherSnapshot, areaName: String?, now: Instant) {
        val key = location.cacheKey()
        val saved = repository.load()
        val unchanged = saved?.let { it.locationKey == key && it.snapshot == snapshot && it.areaName == areaName }
        if (unchanged != true) repository.save(LastWeather(snapshot, now, areaName, key))
    }

    suspend fun clear() = repository.clear()

    // 위치 설정을 그대로 글로 바꾼 값이다. 위치 고르는 방식이 바뀌어 값이 달라지면 예전 날씨는 맞지 않는 것으로 보고 버린다.
    private fun WeatherLocation.cacheKey(): String = toString()
}
