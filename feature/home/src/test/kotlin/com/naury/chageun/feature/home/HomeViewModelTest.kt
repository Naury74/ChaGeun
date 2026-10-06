package com.naury.chageun.feature.home

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.FuelType
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
import com.naury.chageun.core.testing.FakeAuthRepository
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehiclePhotoRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val viewModel = HomeViewModel(
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
        authRepository = auth,
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
        // 고정 시각 UTC 00:00은 밤이다.
        assertThat(content.dayPart).isEqualTo(DayPart.Night)
    }
}
