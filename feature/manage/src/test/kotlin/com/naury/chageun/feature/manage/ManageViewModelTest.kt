package com.naury.chageun.feature.manage

import androidx.lifecycle.SavedStateHandle
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
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ManageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 1)
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = ManageViewModel(
        savedStateHandle = handle,
        vehicleRepository = vehicles,
        observeMaintenanceOverview = ObserveMaintenanceOverviewUseCase(
            maintenance,
            RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
            VehicleHealthAggregator(),
            clock,
        ),
        maintenanceRepository = maintenance,
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(45_000)))
        maintenance.inputs.value = MaintenanceInputs(
            rules = listOf(
                MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.Tire, intervalKm = 50_000, intervalMonths = 48),
                MaintenanceRule(MaintenanceItem.Wiper, intervalKm = null, intervalMonths = 12),
            ),
            lastServices = mapOf(
                MaintenanceItem.EngineOil to ServiceRecord(today.minusMonths(6), Kilometers(35_100)),
                MaintenanceItem.Tire to ServiceRecord(today.minusYears(1), Kilometers(30_000)),
            ),
            mileageHistory = listOf(MileageReading(today, Kilometers(45_000))),
        )
    }

    @Test
    fun countsAndFiltersItems() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        val all = vm.uiState.first { !it.isLoading }
        vm.selectFilter(ManageFilter.NeedsAttention)
        val attention = vm.uiState.first { it.filter == ManageFilter.NeedsAttention }

        assertThat(
            all.counts,
        ).containsExactly(ManageFilter.All, 3, ManageFilter.NeedsAttention, 1, ManageFilter.Upcoming, 0)
        assertThat(attention.items.map { it.item }).containsExactly(MaintenanceItem.EngineOil)
    }

    @Test
    fun showsDetailWithHistory_forSelectedItem() = runTest {
        maintenance.history.value = mapOf(
            MaintenanceItem.EngineOil to listOf(
                ServiceHistoryEntry(
                    "r1",
                    today.minusMonths(6),
                    Kilometers(35_100),
                    costWon = 95_000,
                    shopName = "Shop",
                ),
            ),
        )
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.selectItem(MaintenanceItem.EngineOil)
        val detail = vm.uiState.first { it.detail != null }.detail!!

        assertThat(detail.status.item).isEqualTo(MaintenanceItem.EngineOil)
        assertThat(detail.rule?.intervalKm).isEqualTo(10_000)
        assertThat(detail.history.single().costWon).isEqualTo(95_000)
    }

    @Test
    fun keepsSelectionAndFilter_acrossRecreation() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            selectFilter(ManageFilter.NeedsAttention)
            selectItem(MaintenanceItem.EngineOil)
        }

        val restored = viewModel(handle)
        backgroundScope.launch { restored.uiState.collect {} }
        val state = restored.uiState.first { !it.isLoading }

        assertThat(state.filter).isEqualTo(ManageFilter.NeedsAttention)
        assertThat(state.selectedItem).isEqualTo(MaintenanceItem.EngineOil)
    }

    @Test
    fun clearsDetail_whenSelectionCleared() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.selectItem(MaintenanceItem.Tire)
        vm.uiState.first { it.detail != null }

        vm.selectItem(null)

        assertThat(vm.uiState.first { it.selectedItem == null }.detail).isNull()
    }
}
