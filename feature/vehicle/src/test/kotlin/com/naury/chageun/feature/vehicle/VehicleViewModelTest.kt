package com.naury.chageun.feature.vehicle

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.vehicle.CompleteInspectionUseCase
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAlbumRepository
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeReminderNotifier
import com.naury.chageun.core.testing.FakeVehiclePhotoRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class VehicleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository()
    private val inspections = FakeInspectionRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val history = FakeHistoryRepository()
    private val notifier = FakeReminderNotifier()
    private val photos = FakeVehiclePhotoRepository()

    private fun viewModel() = VehicleViewModel(
        vehicles,
        inspections,
        CompleteInspectionUseCase(
            AddHistoryRecordUseCase(history, FakeMaintenanceRepository(), clock),
            inspections,
            notifier,
        ),
        photos,
        album,
        clock,
    )

    private val album = FakeAlbumRepository()

    @Test
    fun exposesNewestMileageAsCurrent() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Diesel, Kilometers(40_000)))
        vehicles.mileageLog.value = listOf(
            MileageEntry("m2", LocalDate.of(2026, 10, 1), Kilometers(43_000), MileageSource.Fuel),
            MileageEntry("m1", LocalDate.of(2026, 9, 1), Kilometers(40_000), MileageSource.User),
        )

        val state = viewModel.uiState.first {
            it is VehicleUiState.Content && it.mileageLog.size == 2
        } as VehicleUiState.Content

        assertThat(state.currentMileage?.mileage).isEqualTo(Kilometers(43_000))
        assertThat(state.vehicle.fuelType).isEqualTo(FuelType.Diesel)
    }

    @Test
    fun inspectionDate_isEvaluatedAndCanBeCleared() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Diesel, Kilometers(40_000)))
        viewModel.uiState.first { it is VehicleUiState.Content }

        viewModel.setInspectionDate(LocalDate.of(2026, 10, 15))
        val dueSoon = viewModel.uiState.first {
            (it as? VehicleUiState.Content)?.inspection?.schedule != null
        } as VehicleUiState.Content
        assertThat(dueSoon.inspection.state).isEqualTo(InspectionState.DueSoon)
        assertThat(dueSoon.inspection.daysLeft).isEqualTo(14)

        viewModel.setInspectionDate(null)
        val cleared = viewModel.uiState.first {
            (it as? VehicleUiState.Content)?.inspection?.schedule == null
        } as VehicleUiState.Content
        assertThat(cleared.inspection.state).isEqualTo(InspectionState.Unknown)
    }

    @Test
    fun completeInspection_recordsCheck_andMovesDueDate() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Diesel, Kilometers(40_000)))
        viewModel.uiState.first { it is VehicleUiState.Content }

        viewModel.completeInspection(
            InspectionCompletion(LocalDate.of(2026, 9, 30), Kilometers(41_000), LocalDate.of(2028, 9, 30)),
            title = "Periodic inspection",
        )
        val state = viewModel.uiState.first {
            (it as? VehicleUiState.Content)?.inspection?.schedule != null
        } as VehicleUiState.Content

        assertThat(state.inspection.schedule?.nextDueDate).isEqualTo(LocalDate.of(2028, 9, 30))
        assertThat(history.addedChecks.single().first.title).isEqualTo("Periodic inspection")
        assertThat(notifier.inspectionCancelCount).isEqualTo(1)
    }

    @Test
    fun photo_isShown_andFailedImportIsReported() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Diesel, Kilometers(40_000)))
        viewModel.uiState.first { it is VehicleUiState.Content }

        viewModel.setPhoto("car")
        val withPhoto = viewModel.uiState.first {
            (it as? VehicleUiState.Content)?.photoPath != null
        } as VehicleUiState.Content
        assertThat(withPhoto.photoPath).isEqualTo("/photos/car.jpg")

        photos.importSucceeds = false
        viewModel.setPhoto("broken")
        val failed = viewModel.uiState.first { (it as? VehicleUiState.Content)?.isPhotoImportFailed == true }
        assertThat((failed as VehicleUiState.Content).photoPath).isEqualTo("/photos/car.jpg")

        viewModel.removePhoto()
        val removed = viewModel.uiState.first { (it as? VehicleUiState.Content)?.photoPath == null }
        assertThat(removed).isInstanceOf(VehicleUiState.Content::class.java)
    }

    @Test
    fun previewsNewestAlbumPhotos() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Diesel, Kilometers(40_000)))

        album.add(VehicleId("vehicle-1"), (1..6).map { "content://$it" }, LocalDate.of(2026, 10, 1))

        val state = viewModel.uiState.first {
            it is VehicleUiState.Content && it.albumCount > 0
        } as VehicleUiState.Content
        assertThat(state.albumCount).isEqualTo(6)
        assertThat(state.albumPreview).hasSize(4)
    }
}
