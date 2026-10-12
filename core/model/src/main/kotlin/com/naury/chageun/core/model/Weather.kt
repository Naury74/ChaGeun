package com.naury.chageun.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/** 홈 인사말과 운전 팁에 쓰는 날씨 상태. 제공처의 세부 코드를 이 정도로만 묶는다. */
enum class WeatherCondition {
    Clear,
    PartlyCloudy,
    Cloudy,
    Fog,
    Rain,
    HeavyRain,
    Thunder,
    Sleet,
    Snow,
}

/**
 * 한 지점의 지금 날씨. [temperatureC]는 섭씨, [windSpeedMs]는 초속 m다.
 * [isNight]는 제공처가 밤 아이콘을 줬을 때 true라 맑은 밤에 해 대신 달을 그린다.
 */
data class WeatherSnapshot(
    val condition: WeatherCondition,
    val temperatureC: Double,
    val windSpeedMs: Double,
    val isNight: Boolean,
    val observedAt: Instant,
)

/** 위도·경도. 기기 위치를 그대로 담을 수 있으니 저장하거나 로그에 남기지 않는다. */
data class GeoPoint(val latitude: Double, val longitude: Double) {

    /**
     * 소수 첫째 자리(약 10km)로 반올림한 지점. 날씨를 물을 때는 이 값만 보낸다.
     * 같은 동네라면 같은 값이 되어 캐시도 함께 쓴다.
     */
    fun roundedForWeather(): GeoPoint = GeoPoint(latitude.roundTo(1), longitude.roundTo(1))

    /**
     * 소수 둘째 자리(약 1km)로 반올림한 지점. 사용자가 직접 고른 읍·면·동의 대표 지점에만 쓴다.
     * 기기 위치가 아니라 누구나 아는 행정구역의 지점이라 [roundedForWeather]보다 좁게 보내도 된다.
     */
    fun roundedForChosenArea(): GeoPoint = GeoPoint(latitude.roundTo(2), longitude.roundTo(2))

    private fun Double.roundTo(scale: Int): Double =
        BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP).toDouble()
}

/** 날씨를 볼 위치. 대략적인 기기 위치이거나 직접 고른 시·도, 시·군·구, 읍·면·동이다. */
sealed interface WeatherLocation {
    data object Current : WeatherLocation

    data class Region(val area: KoreaArea) : WeatherLocation
}

/**
 * 홈 날씨 설정. 기본은 켜져 있지만 [location]을 고르기 전에는 위치를 묻지도, 날씨를 받지도 않는다.
 * 홈 카드의 "날씨 보기"를 눌러 위치를 고를 때 처음 위치 권한을 묻는다.
 */
data class WeatherSettings(
    val isEnabled: Boolean = true,
    val location: WeatherLocation? = null,
    /** 홈의 "날씨 맞춤 운전 팁 켜기" 안내를 닫았다. 그 뒤로는 설정에서만 켤 수 있다. */
    val isPromptDismissed: Boolean = false,
)
