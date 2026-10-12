package com.naury.chageun.feature.home

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.ads.AppUsage
import com.naury.chageun.core.domain.greeting.GreetingSituation
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.domain.weather.LastWeather
import com.naury.chageun.core.domain.weather.LastWeatherCache
import com.naury.chageun.core.domain.weather.LoadWeatherUseCase
import com.naury.chageun.core.domain.weather.WeatherRepository
import com.naury.chageun.core.domain.weather.WeatherTip
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.GeoPoint
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
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
import com.naury.chageun.core.testing.FakeLastWeatherRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeProfilePhotoRepository
import com.naury.chageun.core.testing.FakeVehiclePhotoRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.FakeWeatherRepository
import com.naury.chageun.core.testing.FakeWeatherSettingsRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import com.naury.chageun.core.testing.TestKoreaRegions
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val history = FakeHistoryRepository()
    private val photos = FakeVehiclePhotoRepository()
    private val auth = FakeAuthRepository()
    private val profilePhotos = FakeProfilePhotoRepository()
    private val usage = FakeAppUsageRepository(AppUsage(firstLaunchAt = Instant.EPOCH, launchCount = 5))
    private val weatherSettings = FakeWeatherSettingsRepository()
    private val location = FakeApproximateLocationProvider()
    private val weatherRepository = FakeWeatherRepository()
    private val areaNames = FakeAreaNameRepository()
    private val lastWeather = FakeLastWeatherRepository()
    private val viewModel = homeViewModel(weatherRepository)

    private fun homeViewModel(weather: WeatherRepository) = HomeViewModel(
        vehicleRepository = vehicles,
        observeMaintenanceOverview = ObserveMaintenanceOverviewUseCase(
            maintenance,
            FakeInspectionRepository(),
            RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
            VehicleHealthAggregator(),
            clock,
        ),
        historyRepository = history,
        photoRepository = photos,
        header = HomeHeaderSource(
            auth,
            profilePhotos,
            usage,
            HomeWeatherSources(
                weatherSettings,
                LoadWeatherUseCase(location, weather, areaNames, weatherSettings),
                LastWeatherCache(lastWeather),
                FakeKoreaRegionRepository(),
            ),
            clock,
        ),
        clock = clock,
    )

    @Test
    fun showsCutoutProgress_andRetriesBackgroundRemoval() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))
        photos.photo.value = "/photos/car.jpg"
        photos.cutout.value = CutoutStatus.DownloadingModel(0.3f)

        val downloading = viewModel.uiState.first {
            it is HomeUiState.Content && it.cutout is CutoutStatus.DownloadingModel
        } as HomeUiState.Content
        viewModel.removeBackground()

        assertThat(downloading.cutout).isEqualTo(CutoutStatus.DownloadingModel(0.3f))
        assertThat(photos.removeBackgroundCalls).isEqualTo(1)
    }

    @Test
    fun staysLoading_untilVehicleExists() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value).isEqualTo(HomeUiState.Loading)
    }

    @Test
    fun recalculates_whenServiceIsRecorded() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))
        val rule = MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12)
        val mileage = listOf(MileageReading(TODAY, Kilometers(45_000)))
        maintenance.inputs.value = MaintenanceInputs(listOf(rule), emptyMap(), mileage)

        val before = viewModel.uiState.first { it is HomeUiState.Content } as HomeUiState.Content
        maintenance.inputs.value = MaintenanceInputs(
            listOf(rule),
            mapOf(MaintenanceItem.EngineOil to ServiceRecord(TODAY.minusMonths(1), Kilometers(44_000))),
            mileage,
        )
        val after = viewModel.uiState.first { it is HomeUiState.Content && it.goodCount == 1 } as HomeUiState.Content

        assertThat(before.missingInfo.single().state).isEqualTo(MaintenanceState.Unknown)
        assertThat(after.vehicle.model).isEqualTo("Model")
    }

    @Test
    fun showsThreeMostRecentRecords_andPromptsForStaleMileage() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))
        maintenance.inputs.value =
            MaintenanceInputs(emptyList(), emptyMap(), listOf(MileageReading(TODAY.minusDays(30), Kilometers(45_000))))
        history.timeline.value = List(5) { index ->
            TimelineItem(
                ref = RecordRef(TimelineEventType.Note, "note-$index"),
                date = TODAY.minusDays(index.toLong()),
                title = "Note $index",
                maintenanceItem = null,
                mileage = null,
                costWon = null,
                source = RecordSource.User,
                createdAt = Instant.EPOCH,
            )
        }

        val state = viewModel.uiState.first {
            it is HomeUiState.Content && it.recentRecords.isNotEmpty()
        } as HomeUiState.Content

        assertThat(state.recentRecords.map { it.title }).containsExactly("Note 0", "Note 1", "Note 2").inOrder()
        assertThat(state.needsMileageUpdate).isTrue()
    }

    @Test
    fun greetsVerifiedUserByName_atTimeOfDay() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Kia", "Sportage", 2023, FuelType.Hybrid, Kilometers(40_000)))
        assertThat((viewModel.uiState.first { it is HomeUiState.Content } as HomeUiState.Content).greetingName).isNull()

        auth.signUpWithEmail("naury@example.com", "chageun1")
        // 인증 전에는 아직 가입 전이라 이름을 부르지 않는다.
        assertThat((viewModel.uiState.value as HomeUiState.Content).greetingName).isNull()

        auth.verifyEmailOutside("naury@example.com")
        auth.reload()
        val content = viewModel.uiState.first {
            (it as? HomeUiState.Content)?.greetingName != null
        } as HomeUiState.Content
        assertThat(content.greetingName).isEqualTo("naury")
        // 고정 시각 UTC 00:00은 새벽이다.
        assertThat(content.greeting.situation).isEqualTo(GreetingSituation.Dawn)
    }

    @Test
    fun googleSignIn_showsGoogleNameAndPhoto_rightAway() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Kia", "Sportage", 2023, FuelType.Hybrid, Kilometers(40_000)))
        viewModel.uiState.first { it is HomeUiState.Content }

        auth.signInWithGoogle("token")
        profilePhotos.photoPath.value = "/files/profile/avatar.jpg"
        val content = viewModel.uiState.first {
            (it as? HomeUiState.Content)?.avatarPath != null
        } as HomeUiState.Content

        assertThat(content.greetingName).isEqualTo("Driver")
        assertThat(content.avatarPath).isEqualTo("/files/profile/avatar.jpg")
    }

    @Test
    fun upcomingEvents_coverTheNextTwoWeeks_atMostThree() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))
        val today = java.time.LocalDate.of(2026, 10, 1)
        maintenance.inputs.value = MaintenanceInputs(
            rules = listOf(
                MaintenanceRule(MaintenanceItem.Wiper, intervalKm = null, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.CabinFilter, intervalKm = null, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.Battery, intervalKm = null, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.Coolant, intervalKm = null, intervalMonths = 12),
            ),
            lastServices = mapOf(
                MaintenanceItem.Wiper to ServiceRecord(today.minusMonths(13), null),
                MaintenanceItem.CabinFilter to ServiceRecord(today.minusMonths(12).plusDays(5), null),
                MaintenanceItem.Battery to ServiceRecord(today.minusMonths(12).plusDays(10), null),
                MaintenanceItem.Coolant to ServiceRecord(today.minusMonths(12).plusDays(30), null),
            ),
            mileageHistory = listOf(MileageReading(today, Kilometers(45_000))),
        )

        val events = (
            viewModel.uiState.first {
                it is HomeUiState.Content && it.upcomingEvents.size == 3
            } as HomeUiState.Content
            ).upcomingEvents

        assertThat(events.map { it.date }).containsExactly(today, today.plusDays(5), today.plusDays(10)).inOrder()
        assertThat(events.first().isOverdue).isTrue()
    }

    @Test
    fun firstLaunch_welcomesUser() = runTest {
        usage.usage.value = AppUsage(firstLaunchAt = Instant.EPOCH, launchCount = 1)
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))

        val content = viewModel.uiState.first { it is HomeUiState.Content } as HomeUiState.Content

        assertThat(content.greeting.situation).isEqualTo(GreetingSituation.FirstVisit)
    }

    @Test
    fun weather_asksForLocationFirst_thenShowsTipForPickedRegion() = runTest {
        weatherRepository.snapshot = WeatherSnapshot(WeatherCondition.Rain, 17.9, 3.4, false, Instant.EPOCH)
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))

        assertThat(contentWith { it.weather == HomeWeather.Setup }.weather).isEqualTo(HomeWeather.Setup)
        // 위치를 고르기 전에는 날씨를 묻지 않는다.
        assertThat(weatherRepository.requested).isEmpty()

        viewModel.setWeatherLocation(WeatherLocation.Region(TestKoreaRegions.seoul))
        val shown = contentWith { it.weather is HomeWeather.Shown }.weather as HomeWeather.Shown

        assertThat(shown.tip).isEqualTo(WeatherTip.Rain)
        assertThat(shown.location).isEqualTo(WeatherLocation.Region(TestKoreaRegions.seoul))
    }

    @Test
    fun weather_currentLocationWithoutPermission_asksAgain_andFailureHidesCard() = runTest {
        weatherSettings.settings.value = WeatherSettings(location = WeatherLocation.Current)
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))

        assertThat(contentWith { it.weather == HomeWeather.NeedsPermission }.weather)
            .isEqualTo(HomeWeather.NeedsPermission)

        location.permitted = true
        location.location = GeoPoint(37.56, 126.97)
        viewModel.refreshWeather()

        // 제공처가 답하지 않으면 오류 대신 카드를 숨긴다.
        assertThat(contentWith { it.weather == HomeWeather.Unavailable }.weather).isEqualTo(HomeWeather.Unavailable)
        assertThat(weatherRepository.requested.single()).isEqualTo(GeoPoint(37.6, 127.0))

        weatherSettings.setEnabled(false)
        assertThat(contentWith { it.weather == HomeWeather.Off }.weather).isEqualTo(HomeWeather.Off)
    }

    @Test
    fun weather_currentLocation_showsAreaName_foundWithRoundedPoint() = runTest {
        weatherRepository.snapshot = WeatherSnapshot(WeatherCondition.Cloudy, 27.0, 1.0, false, Instant.EPOCH)
        areaNames.areaName = "서울 강남구"
        location.permitted = true
        location.location = GeoPoint(37.49794, 127.02762)
        weatherSettings.settings.value = WeatherSettings(location = WeatherLocation.Current)
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))

        val shown = contentWith { it.weather is HomeWeather.Shown }.weather as HomeWeather.Shown

        assertThat(shown.areaName).isEqualTo("서울 강남구")
        assertThat(areaNames.requested.single()).isEqualTo(GeoPoint(37.5, 127.0))
        assertThat(weatherSettings.currentAreaName.value).isEqualTo("서울 강남구")
    }

    @Test
    fun weather_locationFailure_isToldApart_fromWeatherFailure_andRetriesWhenShownAgain() = runTest {
        weatherRepository.snapshot = WeatherSnapshot(WeatherCondition.Cloudy, 27.0, 1.0, false, Instant.EPOCH)
        location.permitted = true
        location.location = null
        weatherSettings.settings.value = WeatherSettings(location = WeatherLocation.Current)
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))

        // 기기가 위치를 주지 못하면 날씨는 묻지도 않고 지역을 고르라고 안내한다.
        assertThat(contentWith { it.weather == HomeWeather.LocationUnavailable }.weather)
            .isEqualTo(HomeWeather.LocationUnavailable)
        assertThat(weatherRepository.requested).isEmpty()
        assertThat(weatherSettings.isCurrentLocationMissing.value).isTrue()

        // 홈이 다시 보일 때 다시 찾는다. 이번엔 위치는 찾았지만 제공처가 실패하면 조용히 숨긴다.
        location.location = GeoPoint(37.56, 126.97)
        weatherRepository.snapshot = null
        viewModel.refreshWeather()
        assertThat(contentWith { it.weather == HomeWeather.Unavailable }.weather).isEqualTo(HomeWeather.Unavailable)
        assertThat(weatherSettings.isCurrentLocationMissing.value).isFalse()
    }

    @Test
    fun savedWeather_showsOnFirstFrame_thenFreshWeatherReplacesIt() = runTest {
        val seoul = WeatherLocation.Region(TestKoreaRegions.seoul)
        weatherSettings.settings.value = WeatherSettings(location = seoul)
        val saved = WeatherSnapshot(WeatherCondition.Snow, -1.0, 2.0, false, Instant.EPOCH)
        lastWeather.saved =
            LastWeather(saved, clock.instant().minusSeconds(3_600), null, LastWeatherCache.keyFor(seoul))
        val fresh = CompletableDeferred<WeatherSnapshot?>()
        val viewModel = homeViewModel(GatedWeatherRepository(fresh))
        val seen = mutableListOf<HomeWeather>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { (it as? HomeUiState.Content)?.let { content -> seen += content.weather } }
        }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))
        viewModel.uiState.first { it is HomeUiState.Content }

        // 기본 하늘(Off·Loading)을 거치지 않고 저장해 둔 눈 날씨부터 그린다.
        assertThat((seen.first() as HomeWeather.Shown).snapshot).isEqualTo(saved)
        assertThat((seen.first() as HomeWeather.Shown).scene).isEqualTo(WeatherScene.Snow)

        val rain = WeatherSnapshot(WeatherCondition.Rain, 12.0, 3.0, false, Instant.EPOCH)
        fresh.complete(rain)
        viewModel.uiState.first { ((it as? HomeUiState.Content)?.weather as? HomeWeather.Shown)?.snapshot == rain }
        assertThat(lastWeather.saved?.snapshot).isEqualTo(rain)
        assertThat(lastWeather.saved?.fetchedAt).isEqualTo(clock.instant())
    }

    @Test
    fun savedWeather_olderThanThreeHours_isNotShown() = runTest {
        val seoul = WeatherLocation.Region(TestKoreaRegions.seoul)
        weatherSettings.settings.value = WeatherSettings(location = seoul)
        val saved = WeatherSnapshot(WeatherCondition.Snow, -1.0, 2.0, false, Instant.EPOCH)
        lastWeather.saved =
            LastWeather(saved, clock.instant().minusSeconds(3 * 3_600), null, LastWeatherCache.keyFor(seoul))
        val viewModel = homeViewModel(GatedWeatherRepository(CompletableDeferred()))
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))

        val content = viewModel.uiState.first { it is HomeUiState.Content } as HomeUiState.Content

        assertThat(content.weather).isEqualTo(HomeWeather.Loading)
        assertThat(lastWeather.saved).isNull()
    }

    @Test
    fun weatherFailsAfterRestart_keepsSavedWeather_andTurningOffClearsIt() = runTest {
        val seoul = WeatherLocation.Region(TestKoreaRegions.seoul)
        weatherSettings.settings.value = WeatherSettings(location = seoul)
        val saved = WeatherSnapshot(WeatherCondition.Cloudy, 15.0, 2.0, false, Instant.EPOCH)
        lastWeather.saved = LastWeather(saved, clock.instant().minusSeconds(600), null, LastWeatherCache.keyFor(seoul))
        weatherRepository.snapshot = null
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))

        // 새로 받지 못해도 아직 쓸 만한 날씨는 숨기지 않는다.
        val shown = contentWith { it.weather is HomeWeather.Shown }.weather as HomeWeather.Shown
        assertThat(shown.snapshot).isEqualTo(saved)
        assertThat(weatherRepository.requested).isNotEmpty()

        weatherSettings.setEnabled(false)
        contentWith { it.weather == HomeWeather.Off }
        assertThat(lastWeather.saved).isNull()
    }

    private suspend fun contentWith(predicate: (HomeUiState.Content) -> Boolean): HomeUiState.Content =
        viewModel.uiState.first { it is HomeUiState.Content && predicate(it) } as HomeUiState.Content
}

/** [result]가 정해질 때까지 기다렸다가 돌려준다. 새 날씨를 받는 동안 홈이 무엇을 보이는지 확인한다. */
private class GatedWeatherRepository(private val result: CompletableDeferred<WeatherSnapshot?>) : WeatherRepository {
    override suspend fun weatherAt(point: GeoPoint): WeatherSnapshot? = result.await()
}
