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
    fun roundedForWeather(): GeoPoint = GeoPoint(latitude.roundTo1(), longitude.roundTo1())

    private fun Double.roundTo1(): Double = BigDecimal.valueOf(this).setScale(1, RoundingMode.HALF_UP).toDouble()
}

/** 위치 권한 없이 고를 수 있는 17개 시·도. */
enum class KoreaRegion {
    Seoul,
    Busan,
    Daegu,
    Incheon,
    Gwangju,
    Daejeon,
    Ulsan,
    Sejong,
    Gyeonggi,
    Gangwon,
    Chungbuk,
    Chungnam,
    Jeonbuk,
    Jeonnam,
    Gyeongbuk,
    Gyeongnam,
    Jeju,
    ;

    /** 시청·도청 소재지 근처의 대표 지점. 지역을 고르면 이 좌표의 날씨를 묻는다. */
    val point: GeoPoint get() = REGION_POINTS.getValue(this)
}

private val REGION_POINTS: Map<KoreaRegion, GeoPoint> = mapOf(
    KoreaRegion.Seoul to GeoPoint(37.57, 126.98),
    KoreaRegion.Busan to GeoPoint(35.18, 129.08),
    KoreaRegion.Daegu to GeoPoint(35.87, 128.60),
    KoreaRegion.Incheon to GeoPoint(37.46, 126.71),
    KoreaRegion.Gwangju to GeoPoint(35.16, 126.85),
    KoreaRegion.Daejeon to GeoPoint(36.35, 127.38),
    KoreaRegion.Ulsan to GeoPoint(35.54, 129.31),
    KoreaRegion.Sejong to GeoPoint(36.48, 127.29),
    KoreaRegion.Gyeonggi to GeoPoint(37.27, 127.01),
    KoreaRegion.Gangwon to GeoPoint(37.88, 127.73),
    KoreaRegion.Chungbuk to GeoPoint(36.64, 127.49),
    KoreaRegion.Chungnam to GeoPoint(36.66, 126.67),
    KoreaRegion.Jeonbuk to GeoPoint(35.82, 127.15),
    KoreaRegion.Jeonnam to GeoPoint(34.82, 126.46),
    KoreaRegion.Gyeongbuk to GeoPoint(36.58, 128.51),
    KoreaRegion.Gyeongnam to GeoPoint(35.24, 128.69),
    KoreaRegion.Jeju to GeoPoint(33.50, 126.53),
)

/** 날씨를 볼 위치. 대략적인 기기 위치이거나 직접 고른 시·도다. */
sealed interface WeatherLocation {
    data object Current : WeatherLocation

    data class Region(val region: KoreaRegion) : WeatherLocation
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
