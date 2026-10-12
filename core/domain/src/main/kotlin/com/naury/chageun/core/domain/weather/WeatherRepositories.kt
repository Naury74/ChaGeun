package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * 홈 날씨를 켤지, 어느 위치의 날씨를 볼지. 좌표는 저장하지 않고 "현재 위치"인지, 고른 지역의 행정구역코드만 둔다.
 */
interface WeatherSettingsRepository {
    val settings: Flow<WeatherSettings>

    /** 최근에 직접 고른 지역의 코드. 새로 고른 것이 앞이고 다섯 개까지 둔다. */
    val recentRegionCodes: Flow<List<String>>

    /** 설정 › 데이터 › 모두 삭제 때 최근에 고른 지역을 지운다. 지금 고른 위치는 그대로 둔다. */
    suspend fun clearRecentRegions()

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setLocation(location: WeatherLocation)

    /** 홈의 날씨 켜기 안내를 닫는다. */
    suspend fun dismissPrompt()

    /**
     * "현재 위치"로 마지막에 찾은 동네 이름(예: "서울 강남구"). 시·군·구까지만 담아 좌표보다 넓고,
     * 다음 실행 때 설정 › 날씨에 바로 보이도록 둔다. 날씨를 끄거나 위치를 바꾸면 지워진다.
     */
    val currentAreaName: Flow<String?>

    /** 날씨가 켜져 있고 위치가 "현재 위치"일 때만 저장한다. null이면 지운다. */
    suspend fun setCurrentAreaName(name: String?)

    /**
     * "현재 위치"를 골랐지만 지난번에 기기가 위치를 주지 못했다(위치 꺼짐·시간 초과). 설정에 "찾지 못함"으로 보인다.
     * 날씨를 끄거나 위치를 바꾸거나 다음에 위치를 찾으면 지워진다.
     */
    val isCurrentLocationMissing: Flow<Boolean>

    /** 날씨가 켜져 있고 위치가 "현재 위치"일 때만 true를 저장한다. */
    suspend fun setCurrentLocationMissing(missing: Boolean)

    /**
     * 새 지역 목록 때문에 고른 지역이 목록에서 없어졌다. 위 시·군·구나 시·도의 날씨를 보여 주면서 홈에
     * "다시 고르기" 안내를 한 번 띄운다. 닫거나 위치를 다시 고르면 사라진다.
     */
    val isRegionChangeNoticeVisible: Flow<Boolean>

    suspend fun dismissRegionChangeNotice()

    /**
     * [version] 지역 목록을 새로 쓰기 시작한 뒤 한 번 부른다. 고른 지역과 최근 지역의 코드를 [renamedCodes]에 따라
     * 새 코드로 바꾸고, 고른 지역이 [catalog]에 없으면 안내를 켠다. 같은 판이나 더 옛 판으로 다시 부르면 아무것도 하지 않는다.
     */
    suspend fun applyRegionListChange(version: String, renamedCodes: Map<String, String>, catalog: KoreaRegionCatalog)
}

/** 대략적인 기기 위치(ACCESS_COARSE_LOCATION). 권한이 없거나 위치를 못 구하면 null이다. */
interface ApproximateLocationProvider {
    fun hasPermission(): Boolean

    suspend fun currentLocation(): GeoPoint?
}

/**
 * 반올림한 [point]가 속한 동네 이름(시·도와 시·군·구까지, 예: "서울 강남구"). 기기의 지오코더로 찾고,
 * 찾지 못하면 null이라 화면은 "현재 위치"라고만 쓴다. 구현은 같은 지점을 다시 묻지 않도록 캐시한다.
 */
interface AreaNameRepository {
    suspend fun areaNameAt(point: GeoPoint): String?
}

/**
 * [point]의 지금 날씨. 네트워크가 없거나 제공처가 실패하면 null이고, 홈은 날씨 없이 인사한다.
 * 구현은 같은 지점을 짧은 간격으로 다시 묻지 않도록 [WeatherCachePolicy]에 따라 캐시한다.
 */
interface WeatherRepository {
    suspend fun weatherAt(point: GeoPoint): WeatherSnapshot?
}
