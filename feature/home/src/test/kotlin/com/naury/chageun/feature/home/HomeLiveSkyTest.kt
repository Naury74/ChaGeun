package com.naury.chageun.feature.home

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.greeting.GreetingSituation
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.domain.weather.DaylightRules
import com.naury.chageun.core.domain.weather.LastWeatherCache
import com.naury.chageun.core.domain.weather.LoadWeatherUseCase
import com.naury.chageun.core.domain.weather.SolarCalculator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.model.WeatherSnapshot
import com.naury.chageun.core.testing.FakeAppUsageRepository
import com.naury.chageun.core.testing.FakeApproximateLocationProvider
import com.naury.chageun.core.testing.FakeAreaNameRepository
import com.naury.chageun.core.testing.FakeAuthRepository
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeKoreaRegionRepository
import com.naury.chageun.core.testing.FakeKoreaRegionUpdates
import com.naury.chageun.core.testing.FakeLastWeatherRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeProfilePhotoRepository
import com.naury.chageun.core.testing.FakeTimeChangeSource
import com.naury.chageun.core.testing.FakeVehiclePhotoRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.FakeWeatherRepository
import com.naury.chageun.core.testing.FakeWeatherSettingsRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import com.naury.chageun.core.testing.TestKoreaRegions
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** 홈을 켜 둔 채 해가 지거나 시계를 바꿨을 때, 날씨를 다시 받지 않고 하늘과 인사말만 바뀌는지 본다. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeLiveSkyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Seoul")

    // 2026년 10월 20일(화) 서울은 17시 50분쯤 해가 진다. 16시 50분에 홈을 연다.
    private val day = LocalDate.of(2026, 10, 20)
    private val start = ZonedDateTime.of(day, LocalTime.of(16, 50), zone).toInstant()
    private val clock = SchedulerClock(start, zone) { mainDispatcherRule.testDispatcher.scheduler.currentTime }
    private val seoul = WeatherLocation.Region(TestKoreaRegions.seoul)
    private val sun = checkNotNull(
        SolarCalculator.sunTimes(TestKoreaRegions.seoul.point.roundedForChosenArea(), day),
    )
    private val vehicles = FakeVehicleRepository()
    private val weatherSettings = FakeWeatherSettingsRepository(WeatherSettings(location = seoul))
    private val weatherRepository =
        FakeWeatherRepository(WeatherSnapshot(WeatherCondition.Clear, 18.0, 1.0, isNight = false, observedAt = start))
    private val timeChanges = FakeTimeChangeSource()
    private val header = HomeHeaderSource(
        FakeAuthRepository(),
        FakeProfilePhotoRepository(),
        FakeAppUsageRepository(AppUsage(firstLaunchAt = Instant.EPOCH, launchCount = 5)),
        HomeWeatherSources(
            weatherSettings,
            LoadWeatherUseCase(
                FakeApproximateLocationProvider(),
                weatherRepository,
                FakeAreaNameRepository(),
                weatherSettings,
            ),
            LastWeatherCache(FakeLastWeatherRepository()),
            FakeKoreaRegionRepository(),
            FakeKoreaRegionUpdates(),
        ),
        HomeClockTicks(clock, timeChanges),
        clock,
    )

    // 메인 디스패처를 테스트 디스패처로 바꾼 뒤에 만들어야 viewModelScope가 가상 시간을 따른다.
    private val viewModel by lazy {
        HomeViewModel(
            vehicleRepository = vehicles,
            observeMaintenanceOverview = ObserveMaintenanceOverviewUseCase(
                FakeMaintenanceRepository(),
                FakeInspectionRepository(),
                RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
                VehicleHealthAggregator(),
                clock,
            ),
            historyRepository = FakeHistoryRepository(),
            photoRepository = FakeVehiclePhotoRepository(),
            header = header,
            clock = clock,
        )
    }

    @Test
    fun keepingHomeOpenAcrossSunset_turnsSkyToDuskThenNight_withoutFetchingAgain() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Kia", "Sportage", 2023, FuelType.Hybrid, Kilometers(40_000)))
        runCurrent()

        assertThat(scene()).isEqualTo(WeatherScene.ClearDay)
        assertThat(content().greeting.situation).isEqualTo(GreetingSituation.AutumnAfternoon)

        advanceTo(sun.sunset.minus(DaylightRules.DUSK).plusSeconds(60))
        assertThat(scene()).isEqualTo(WeatherScene.Dusk)
        // 17시가 지나 퇴근길 인사로 바뀌었다.
        assertThat(content().greeting.situation).isEqualTo(GreetingSituation.CommuteEvening)

        advanceTo(sun.sunset.plusSeconds(60))
        assertThat(scene()).isEqualTo(WeatherScene.ClearNight)

        advanceTo(ZonedDateTime.of(day, LocalTime.of(22, 5), zone).toInstant())
        assertThat(content().greeting.situation).isEqualTo(GreetingSituation.Night)
        // 하늘과 인사말은 받아 둔 날씨로만 다시 골랐다.
        assertThat(weatherRepository.requested).hasSize(1)
    }

    @Test
    fun changingTheDeviceClock_redrawsRightAway() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Kia", "Sportage", 2023, FuelType.Hybrid, Kilometers(40_000)))
        runCurrent()
        assertThat(scene()).isEqualTo(WeatherScene.ClearDay)

        // 사용자가 시계를 두 시간 뒤로 옮겼다. 방송이 오기 전에는 그대로다.
        clock.jump = Duration.ofHours(2)
        runCurrent()
        assertThat(scene()).isEqualTo(WeatherScene.ClearDay)

        timeChanges.change()
        runCurrent()
        assertThat(scene()).isEqualTo(WeatherScene.ClearNight)
        assertThat(content().greeting.situation).isEqualTo(GreetingSituation.CommuteEvening)
        assertThat(weatherRepository.requested).hasSize(1)
    }

    @Test
    fun homeNotVisible_stopsListeningAndWaiting() = runTest {
        val screen = backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Kia", "Sportage", 2023, FuelType.Hybrid, Kilometers(40_000)))
        runCurrent()
        assertThat(timeChanges.subscribers.value).isEqualTo(1)

        // 홈 상태와 날씨가 각각 5초씩 더 기다렸다가 멈춘다.
        screen.cancel()
        advanceTimeBy(Duration.ofSeconds(11).toMillis())
        runCurrent()
        assertThat(timeChanges.subscribers.value).isEqualTo(0)

        val readsWhileHidden = clock.reads
        advanceTimeBy(Duration.ofDays(1).toMillis())
        runCurrent()
        assertThat(clock.reads).isEqualTo(readsWhileHidden)
    }

    private fun TestScope.advanceTo(target: Instant) {
        advanceTimeBy(Duration.between(clock.instant(), target).toMillis())
        runCurrent()
    }

    private fun content(): HomeUiState.Content = viewModel.uiState.value as HomeUiState.Content

    private fun scene(): WeatherScene = (content().weather as HomeWeather.Shown).scene
}

/** 테스트 스케줄러의 가상 시간만큼 흐르는 시계. [jump]는 사용자가 기기 시계를 옮긴 만큼이다. */
private class SchedulerClock(
    private val start: Instant,
    private val zone: ZoneId,
    private val elapsedMillis: () -> Long,
) : Clock() {
    var jump: Duration = Duration.ZERO
    var reads = 0
        private set

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = SchedulerClock(start, zone, elapsedMillis)

    override fun instant(): Instant {
        reads++
        return start.plusMillis(elapsedMillis()).plus(jump)
    }
}
