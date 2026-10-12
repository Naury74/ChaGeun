package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSnapshot
import javax.inject.Inject

sealed interface WeatherResult {
    data class Shown(val snapshot: WeatherSnapshot) : WeatherResult

    /** "현재 위치"를 골랐지만 위치 권한이 꺼져 있다. 다시 허용하거나 지역을 고르게 한다. */
    data object NeedsPermission : WeatherResult

    /** 위치나 날씨를 구하지 못했다. 홈은 날씨 카드를 조용히 숨긴다. */
    data object Unavailable : WeatherResult
}

/** 고른 위치의 날씨를 가져온다. 기기 위치는 반올림한 값만 저장소에 넘긴다. */
class LoadWeatherUseCase @Inject constructor(
    private val locationProvider: ApproximateLocationProvider,
    private val weatherRepository: WeatherRepository,
) {
    suspend operator fun invoke(location: WeatherLocation): WeatherResult {
        if (location == WeatherLocation.Current && !locationProvider.hasPermission()) {
            return WeatherResult.NeedsPermission
        }
        val point = when (location) {
            WeatherLocation.Current -> locationProvider.currentLocation()
            is WeatherLocation.Region -> location.region.point
        }
        val snapshot = point?.let { weatherRepository.weatherAt(it.roundedForWeather()) }
        return snapshot?.let(WeatherResult::Shown) ?: WeatherResult.Unavailable
    }
}
