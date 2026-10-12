package com.naury.chageun.feature.home

import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.ads.AppUsageRepository
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.ProfilePhotoRepository
import com.naury.chageun.core.domain.weather.LoadWeatherUseCase
import com.naury.chageun.core.domain.weather.WeatherResult
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import com.naury.chageun.core.domain.weather.WeatherTips
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val settings: WeatherSettingsRepository,
    private val loadWeather: LoadWeatherUseCase,
    private val clock: Clock,
) {
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
            !settings.isEnabled -> emit(HomeWeather.Off)
            location == null -> emit(if (settings.isPromptDismissed) HomeWeather.Off else HomeWeather.Setup)
            else -> {
                // 이미 날씨를 보여 줬으면 새로 받는 동안 이전 카드를 그대로 둬 깜빡이지 않게 한다.
                if (!hasShownWeather) emit(HomeWeather.Loading)
                emit(
                    when (val result = loadWeather(location)) {
                        is WeatherResult.Shown -> HomeWeather.Shown(
                            snapshot = result.snapshot,
                            location = location,
                            tip = WeatherTips.tipFor(result.snapshot),
                            scene = weatherSceneFor(result.snapshot, LocalTime.now(clock).hour),
                        )
                        WeatherResult.NeedsPermission -> HomeWeather.NeedsPermission
                        WeatherResult.Unavailable -> HomeWeather.Unavailable
                    },
                )
            }
        }
    }
}

/** 홈 머리말의 계정 동그라미와 인사말에 쓰는 이름·사진. */
data class HomeProfile(val name: String?, val avatarPath: String?)
