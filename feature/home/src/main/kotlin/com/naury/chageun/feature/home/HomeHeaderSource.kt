package com.naury.chageun.feature.home

import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.ads.AppUsageRepository
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.ProfilePhotoRepository
import com.naury.chageun.core.domain.weather.Daylight
import com.naury.chageun.core.domain.weather.DaylightRules
import com.naury.chageun.core.domain.weather.LastWeatherCache
import com.naury.chageun.core.domain.weather.LoadWeatherUseCase
import com.naury.chageun.core.domain.weather.WeatherResult
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import com.naury.chageun.core.domain.weather.WeatherTips
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.model.WeatherSnapshot
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * 홈 맨 위 머리말(인사말·날씨)에 필요한 것을 모은다. 부를 이름, 앱 실행 기록, 날씨 카드 상태다.
 * 날씨는 설정과 새로 고침 요청을 받아 [HomeWeather]로 바꾼다.
 */
class HomeHeaderSource @Inject constructor(
    authRepository: AuthRepository,
    profilePhotos: ProfilePhotoRepository,
    appUsageRepository: AppUsageRepository,
    weatherSources: HomeWeatherSources,
    private val clock: Clock,
) {
    private val settings = weatherSources.settings
    private val loadWeather = weatherSources.load
    private val lastWeather = weatherSources.saved

    private var hasShownWeather = false

    /** 로그인한 사용자를 부를 이름과 프로필 사진. 로그인하지 않았으면 둘 다 null이다. */
    val profile: Flow<HomeProfile> = combine(
        authRepository.currentUser.map { it?.greetingName() },
        profilePhotos.photoPath,
        ::HomeProfile,
    )

    /** "처음 오셨네요", "오랜만이에요"를 고르는 데 쓰는 실행 기록. */
    val usage: Flow<AppUsage> = appUsageRepository.usage

    /** 설정이 바뀌거나 [refreshes]가 새 값을 내면 다시 가져온다. 저장소가 한 시간 동안은 캐시를 돌려준다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun weather(refreshes: Flow<Int>): Flow<HomeWeather> =
        combine(settings.settings, refreshes) { settings, _ -> settings }
            .flatMapLatest(::weatherFor)
            .onEach { if (it is HomeWeather.Shown) hasShownWeather = true }

    suspend fun setWeatherLocation(location: WeatherLocation) = settings.setLocation(location)

    suspend fun dismissWeatherPrompt() = settings.dismissPrompt()

    private fun weatherFor(settings: WeatherSettings): Flow<HomeWeather> = flow {
        val location = settings.location
        when {
            !settings.isEnabled -> {
                emit(HomeWeather.Off)
                lastWeather.clear()
            }
            location == null -> emit(if (settings.isPromptDismissed) HomeWeather.Off else HomeWeather.Setup)
            else -> emitAll(weatherAt(location))
        }
    }

    /**
     * 앱을 막 켰으면 저장해 둔 날씨(3시간 안)를 먼저 보여 주고 새로 받는다. 새로 받지 못해도 저장해 둔 날씨가
     * 아직 쓸 만하면 그대로 둔다. 이미 날씨를 보여 줬으면 새로 받는 동안 이전 카드를 그대로 둬 깜빡이지 않게 한다.
     */
    private fun weatherAt(location: WeatherLocation): Flow<HomeWeather> = flow {
        if (!hasShownWeather) emit(savedWeather(location) ?: HomeWeather.Loading)
        val weather = when (val result = loadWeather(location)) {
            is WeatherResult.Shown -> {
                lastWeather.remember(location, result.snapshot, result.areaName, clock.instant())
                shown(location, result.snapshot, result.areaName, result.point)
            }
            WeatherResult.Unavailable -> savedWeather(location) ?: HomeWeather.Unavailable
            WeatherResult.NeedsPermission -> HomeWeather.NeedsPermission
            WeatherResult.LocationUnavailable -> HomeWeather.LocationUnavailable
        }
        emit(weather)
    }

    private suspend fun savedWeather(location: WeatherLocation): HomeWeather.Shown? =
        lastWeather.usable(location, clock.instant())?.let { shown(location, it.snapshot, it.areaName, point = null) }

    /**
     * 저장해 둔 날씨는 좌표가 없어 국토 가운데쯤의 해 지는 시각을 쓴다. 새로 받으면 그 지점으로 다시 계산한다.
     * 해가 진 뒤에는 제공처가 낮 아이콘을 줬어도 "드라이브하기 좋은 날" 같은 낮 팁을 고르지 않는다.
     */
    private fun shown(
        location: WeatherLocation,
        snapshot: WeatherSnapshot,
        areaName: String?,
        point: GeoPoint?,
    ): HomeWeather.Shown {
        val daylight =
            DaylightRules.at(point ?: DaylightRules.KOREA_CENTER, clock.instant(), clock.zone, snapshot.isNight)
        return HomeWeather.Shown(
            snapshot = snapshot,
            location = location,
            tip = WeatherTips.tipFor(snapshot.copy(isNight = daylight == Daylight.Night)),
            scene = weatherSceneFor(snapshot, daylight),
            areaName = areaName,
        )
    }
}

/** 홈 날씨 카드가 쓰는 것들. 날씨 설정, 새로 받기, 저장해 둔 마지막 날씨다. */
class HomeWeatherSources @Inject constructor(
    val settings: WeatherSettingsRepository,
    val load: LoadWeatherUseCase,
    val saved: LastWeatherCache,
)

/** 홈 머리말의 계정 동그라미와 인사말에 쓰는 이름·사진. */
data class HomeProfile(val name: String?, val avatarPath: String?)
