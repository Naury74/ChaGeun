package com.naury.chageun.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.calendar.UpcomingEventPlanner
import com.naury.chageun.core.domain.calendar.within
import com.naury.chageun.core.domain.greeting.GreetingEngine
import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.WeatherLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    observeMaintenanceOverview: ObserveMaintenanceOverviewUseCase,
    historyRepository: HistoryRepository,
    private val photoRepository: VehiclePhotoRepository,
    private val header: HomeHeaderSource,
    private val clock: Clock,
) : ViewModel() {

    private val greetingEngine = GreetingEngine()
    private val weatherRefreshes = MutableStateFlow(0)

    /** 날씨를 기다리느라 홈 전체가 늦게 뜨지 않도록 처음에는 카드 없이(Off) 시작한다. */
    private val weather: StateFlow<HomeWeather> = header.weather(weatherRefreshes)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeWeather.Off)

    val uiState: StateFlow<HomeUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            combine(
                observeMaintenanceOverview(vehicle.id),
                historyRepository.observeTimeline(vehicle.id, TimelineQuery(), limit = RECENT_RECORD_COUNT),
                combine(
                    photoRepository.observe(vehicle.id),
                    photoRepository.observeBackgroundRemoval(vehicle.id),
                    photoRepository.observeCutout(vehicle.id),
                    ::HomePhoto,
                ),
                header.greetingName,
                combine(header.usage, weather, ::Pair),
            ) { overview, recent, photo, name, (usage, weather) ->
                val now = LocalDateTime.now(clock)
                val today = now.toLocalDate()
                HomeUiState.Content(
                    vehicle = vehicle,
                    overview = overview,
                    today = today,
                    recentRecords = recent,
                    photoPath = photo.path,
                    cutout = photo.cutout,
                    isBackgroundRemovalEnabled = photo.removeBackground,
                    greetingName = name,
                    greeting = greetingEngine.greet(greetingFacts(now, clock.zone, vehicle, overview, recent, usage)),
                    weather = weather,
                    // 정비 개요를 한 번만 계산하도록 달력과 같은 규칙으로 여기서 바로 고른다.
                    upcomingEvents = UpcomingEventPlanner.plan(overview, today)
                        .within(today, today.plusDays(UPCOMING_DAYS))
                        .take(UPCOMING_COUNT),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Loading)

    /** [sourceUri]는 Photo Picker가 준 content URI다. 실패해도 홈에서는 실루엣이 그대로 남는다. */
    /** [removeBackground]는 사진을 넣기 전 확인 시트에서 고른 값이다. */
    fun setPhoto(sourceUri: String, removeBackground: Boolean) {
        val vehicle = (uiState.value as? HomeUiState.Content)?.vehicle ?: return
        viewModelScope.launch { photoRepository.replace(vehicle.id, sourceUri, removeBackground) }
    }

    /** 배경을 지우지 못한 사진에 다시 시도한다. 모델이 없으면 내려받는 것부터 진행 상태로 보여 준다. */
    fun removeBackground() {
        val vehicle = (uiState.value as? HomeUiState.Content)?.vehicle ?: return
        photoRepository.removeBackground(vehicle.id)
    }

    /** 홈이 다시 보일 때 부른다. 마지막으로 받은 지 한 시간이 안 됐으면 저장소가 캐시를 돌려준다. */
    fun refreshWeather() {
        weatherRefreshes.update { it + 1 }
    }

    /** 홈의 날씨 켜기 안내를 닫는다. 그 뒤로는 설정 › 날씨에서만 켤 수 있다. */
    fun dismissWeatherPrompt() {
        viewModelScope.launch { header.dismissWeatherPrompt() }
    }

    /** 같은 위치를 다시 골라도(예: 권한을 다시 허용) 새로 가져오도록 새로 고침도 함께 한다. */
    fun setWeatherLocation(location: WeatherLocation) {
        viewModelScope.launch {
            header.setWeatherLocation(location)
            refreshWeather()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RECENT_RECORD_COUNT = 3
        const val UPCOMING_DAYS = 14L
        const val UPCOMING_COUNT = 3
    }
}

private data class HomePhoto(val path: String?, val removeBackground: Boolean, val cutout: CutoutStatus)
