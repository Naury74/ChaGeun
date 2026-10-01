package com.naury.chageun.feature.vehicle

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeInspectionRepository
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

    @Test
    fun exposesNewestMileageAsCurrent() = runTest {
        val viewModel = VehicleViewModel(vehicles, inspections, clock)
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
        val viewModel = VehicleViewModel(vehicles, inspections, clock)
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
}
