package com.naury.chageun.feature.home

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeMaintenanceRepository
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
    private val viewModel = HomeViewModel(
        vehicleRepository = vehicles,
        observeMaintenanceOverview = ObserveMaintenanceOverviewUseCase(
            maintenance,
            RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
            VehicleHealthAggregator(),
            clock,
        ),
        clock = clock,
    )

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
}
