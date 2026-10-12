package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

/** 홈 날씨를 켤지, 어느 위치의 날씨를 볼지. 좌표는 저장하지 않고 "현재 위치"인지 고른 시·도만 둔다. */
interface WeatherSettingsRepository {
    val settings: Flow<WeatherSettings>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setLocation(location: WeatherLocation)

    /** 홈의 날씨 켜기 안내를 닫는다. */
    suspend fun dismissPrompt()
}

/** 대략적인 기기 위치(ACCESS_COARSE_LOCATION). 권한이 없거나 위치를 못 구하면 null이다. */
interface ApproximateLocationProvider {
    fun hasPermission(): Boolean

    suspend fun currentLocation(): GeoPoint?
}

/**
 * [point]의 지금 날씨. 네트워크가 없거나 제공처가 실패하면 null이고, 홈은 날씨 없이 인사한다.
 * 구현은 같은 지점을 짧은 간격으로 다시 묻지 않도록 [WeatherCachePolicy]에 따라 캐시한다.
 */
interface WeatherRepository {
    suspend fun weatherAt(point: GeoPoint): WeatherSnapshot?
}
