package com.naury.chageun.feature.home

import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.ads.AppUsageRepository
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.ProfilePhotoRepository
import com.naury.chageun.core.domain.weather.Daylight
import com.naury.chageun.core.domain.weather.DaylightRules
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionRepository
import com.naury.chageun.core.domain.weather.KoreaRegionUpdates
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
import java.time.Instant
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
    private val weatherSources: HomeWeatherSources,
    private val ticks: HomeClockTicks,
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

    /**
     * 설정이 바뀌거나 [refreshes]가 새 값을 내면 다시 가져온다. 저장소가 한 시간 동안은 캐시를 돌려준다.
     * 해가 지거나 인사말 시간대가 바뀌면 받아 둔 날씨로 하늘만 다시 고르고, 날씨는 새로 받지 않는다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun weather(refreshes: Flow<Int>): Flow<HomeSky> = combine(settings.settings, refreshes) { settings, _ -> settings }
        .flatMapLatest(::weatherFor)
        .flatMapLatest { reading ->
            val point = (reading as? WeatherReading.Fetched)?.point ?: DaylightRules.KOREA_CENTER
            ticks.at(point).map { now -> HomeSky(reading.present(now), now) }
        }
        .onEach { if (it.weather is HomeWeather.Shown) hasShownWeather = true }

    suspend fun setWeatherLocation(location: WeatherLocation) = settings.setLocation(location)

    /** 지역 고르기 창에 보여 줄 최근에 고른 지역. */
    val recentRegionCodes: Flow<List<String>> = settings.recentRegionCodes

    /** 지역 고르기 창을 열 때 읽는 전국 지역 목록. */
    suspend fun regionCatalog(): KoreaRegionCatalog = weatherSources.regions.catalog()

    /** 지역 고르기 창을 열 때, 지역 목록을 확인한 지 일주일이 지났으면 새 목록이 있는지 본다. */
    suspend fun checkRegionListIfStale() = weatherSources.updates.checkIfStale()

    /** 새 지역 목록에서 고른 지역이 없어졌다. 홈에 "다시 고르기" 안내를 띄운다. */
    val isRegionChangeNoticeVisible: Flow<Boolean> = settings.isRegionChangeNoticeVisible

    suspend fun dismissRegionChangeNotice() = settings.dismissRegionChangeNotice()

    suspend fun dismissWeatherPrompt() = settings.dismissPrompt()

    private fun weatherFor(settings: WeatherSettings): Flow<WeatherReading> = flow {
        val location = settings.location
        when {
            !settings.isEnabled -> {
                emit(WeatherReading.Fixed(HomeWeather.Off))
                lastWeather.clear()
            }
            location == null ->
                emit(WeatherReading.Fixed(if (settings.isPromptDismissed) HomeWeather.Off else HomeWeather.Setup))
            else -> emitAll(weatherAt(location))
        }
    }

    /**
     * 앱을 막 켰으면 저장해 둔 날씨(3시간 안)를 먼저 보여 주고 새로 받는다. 새로 받지 못해도 저장해 둔 날씨가
     * 아직 쓸 만하면 그대로 둔다. 이미 날씨를 보여 줬으면 새로 받는 동안 이전 카드를 그대로 둬 깜빡이지 않게 한다.
     */
    private fun weatherAt(location: WeatherLocation): Flow<WeatherReading> = flow {
        if (!hasShownWeather) emit(savedWeather(location) ?: WeatherReading.Fixed(HomeWeather.Loading))
        val weather = when (val result = loadWeather(location)) {
            is WeatherResult.Shown -> {
                lastWeather.remember(location, result.snapshot, result.areaName, clock.instant())
                WeatherReading.Fetched(location, result.snapshot, result.areaName, result.point)
            }
            WeatherResult.Unavailable -> savedWeather(location) ?: WeatherReading.Fixed(HomeWeather.Unavailable)
            WeatherResult.NeedsPermission -> WeatherReading.Fixed(HomeWeather.NeedsPermission)
            WeatherResult.LocationUnavailable -> WeatherReading.Fixed(HomeWeather.LocationUnavailable)
        }
        emit(weather)
    }

    private suspend fun savedWeather(location: WeatherLocation): WeatherReading.Fetched? =
        lastWeather.usable(location, clock.instant())
            ?.let { WeatherReading.Fetched(location, it.snapshot, it.areaName, point = null) }

    private fun WeatherReading.present(now: Instant): HomeWeather = when (this) {
        is WeatherReading.Fixed -> weather
        is WeatherReading.Fetched -> shown(now)
    }

    /**
     * 저장해 둔 날씨는 좌표가 없어 국토 가운데쯤의 해 지는 시각을 쓴다. 새로 받으면 그 지점으로 다시 계산한다.
     * 해가 진 뒤에는 제공처가 낮 아이콘을 줬어도 "드라이브하기 좋은 날" 같은 낮 팁을 고르지 않는다.
     */
    private fun WeatherReading.Fetched.shown(now: Instant): HomeWeather.Shown {
        val daylight = DaylightRules.at(
            point ?: DaylightRules.KOREA_CENTER,
            now,
            clock.zone,
            providerSaysNight = snapshot.isNight,
            observedAt = snapshot.observedAt,
        )
        val isNight = daylight == Daylight.Night
        return HomeWeather.Shown(
            snapshot = snapshot,
            location = location,
            tip = WeatherTips.tipFor(snapshot.copy(isNight = isNight)),
            scene = weatherSceneFor(snapshot.copy(isNight = isNight), daylight),
            areaName = areaName,
        )
    }
}

/** 홈 머리말의 날씨 상태와 그것을 고른 시각. 시각이 달라지면 같은 날씨여도 인사말을 다시 고른다. */
data class HomeSky(val weather: HomeWeather, val at: Instant)

/**
 * 날씨를 받아 온 결과. 받은 날씨([Fetched])는 하늘 장면과 팁을 시각마다 다시 고른다.
 * [Fetched.point]는 날씨를 물은 반올림 좌표라 저장하거나 로그에 남기지 않는다.
 */
private sealed interface WeatherReading {
    class Fixed(val weather: HomeWeather) : WeatherReading

    class Fetched(
        val location: WeatherLocation,
        val snapshot: WeatherSnapshot,
        val areaName: String?,
        val point: GeoPoint?,
    ) : WeatherReading
}

/**
 * 홈 날씨 카드가 쓰는 것들. 날씨 설정, 새로 받기, 저장해 둔 마지막 날씨, 지역 고르기 창의 전국 지역 목록과
 * 그 목록의 새 판 확인이다.
 */
class HomeWeatherSources @Inject constructor(
    val settings: WeatherSettingsRepository,
    val load: LoadWeatherUseCase,
    val saved: LastWeatherCache,
    val regions: KoreaRegionRepository,
    val updates: KoreaRegionUpdates,
)

/** 홈 머리말의 계정 동그라미와 인사말에 쓰는 이름·사진. */
data class HomeProfile(val name: String?, val avatarPath: String?)
