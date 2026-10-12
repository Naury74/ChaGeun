package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSnapshot
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

sealed interface WeatherResult {
    /** [areaName]은 "현재 위치"의 동네 이름이다. 지역을 직접 골랐거나 찾지 못했으면 null이다. */
    data class Shown(val snapshot: WeatherSnapshot, val areaName: String? = null) : WeatherResult

    /** "현재 위치"를 골랐지만 위치 권한이 꺼져 있다. 다시 허용하거나 지역을 고르게 한다. */
    data object NeedsPermission : WeatherResult

    /**
     * "현재 위치"이고 권한도 있지만 기기가 위치를 주지 못했다(위치 꺼짐·시간 초과).
     * 날씨 제공처 실패와 달리 사용자가 고칠 수 있으니 홈은 지역을 고르라고 안내한다.
     */
    data object LocationUnavailable : WeatherResult

    /** 날씨를 받지 못했다(네트워크·제공처 실패). 홈은 날씨 카드를 조용히 숨긴다. */
    data object Unavailable : WeatherResult
}

/**
 * 고른 위치의 날씨를 가져온다. 기기 위치는 반올림한 값만 저장소에 넘긴다.
 * "현재 위치"면 같은 반올림 좌표로 동네 이름도 함께 찾아, 날씨와 동시에 기다린다.
 */
class LoadWeatherUseCase @Inject constructor(
    private val locationProvider: ApproximateLocationProvider,
    private val weatherRepository: WeatherRepository,
    private val areaNameRepository: AreaNameRepository,
    private val settingsRepository: WeatherSettingsRepository,
) {
    suspend operator fun invoke(location: WeatherLocation): WeatherResult {
        if (location == WeatherLocation.Current && !locationProvider.hasPermission()) {
            return WeatherResult.NeedsPermission
        }
        val isCurrent = location == WeatherLocation.Current
        val point = pointFor(location)?.roundedForWeather()
        if (isCurrent) settingsRepository.setCurrentLocationMissing(point == null)
        // 시·도는 대표 좌표가 늘 있으니 null은 기기 위치를 못 구한 경우뿐이다.
        if (point == null) return WeatherResult.LocationUnavailable
        val (snapshot, areaName) = coroutineScope {
            val area = if (isCurrent) async { areaNameRepository.areaNameAt(point) } else null
            weatherRepository.weatherAt(point) to area?.await()
        }
        if (areaName != null) settingsRepository.setCurrentAreaName(areaName)
        return snapshot?.let { WeatherResult.Shown(it, areaName) } ?: WeatherResult.Unavailable
    }

    private suspend fun pointFor(location: WeatherLocation): GeoPoint? = when (location) {
        WeatherLocation.Current -> locationProvider.currentLocation()
        is WeatherLocation.Region -> location.region.point
    }
}
