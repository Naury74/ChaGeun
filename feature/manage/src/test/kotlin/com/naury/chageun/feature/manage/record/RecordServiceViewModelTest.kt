package com.naury.chageun.feature.manage.record

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.RecordServiceUseCase
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeReminderNotifier
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RecordServiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 1)
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = RecordServiceViewModel(
        item = MaintenanceItem.EngineOil,
        savedStateHandle = handle,
        vehicleRepository = vehicles,
        maintenanceRepository = maintenance,
        recordService = RecordServiceUseCase(maintenance, clock, FakeAnalyticsTracker(), FakeReminderNotifier()),
        clock = clock,
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(42_000)))
        maintenance.inputs.value = MaintenanceInputs(
            rules = listOf(MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12)),
            lastServices = mapOf(
                MaintenanceItem.EngineOil to ServiceRecord(LocalDate.of(2026, 3, 10), Kilometers(40_260)),
            ),
            mileageHistory = listOf(MileageReading(LocalDate.of(2026, 9, 1), Kilometers(42_891))),
        )
    }

    @Test
    fun prefillsTodayAndCurrentMileage() {
        val state = viewModel().uiState.value

        assertThat(state.date).isEqualTo(today)
        assertThat(state.mileage).isEqualTo("42891")
    }

    @Test
    fun savesAndShowsNextThreshold() {
        val vm = viewModel()
        vm.onCostChanged("0")
        vm.save()

        val state = vm.uiState.value
        assertThat(state.savedResult).isEqualTo(SavedResult(Kilometers(52_891), LocalDate.of(2027, 10, 1)))
        assertThat(maintenance.recordedServices.single().first.costWon).isEqualTo(0)
    }

    @Test
    fun keepsEmptyCostAsUnknown() {
        val vm = viewModel()
        vm.save()

        assertThat(maintenance.recordedServices.single().first.costWon).isNull()
    }

    @Test
    fun requiresMileage() {
        val vm = viewModel()
        vm.onMileageChanged("")
        vm.save()

        assertThat(vm.uiState.value.errors).containsExactly(RecordServiceField.Mileage, RecordServiceError.Required)
        assertThat(maintenance.recordedServices).isEmpty()
    }

    @Test
    fun warnsAboutLowerMileage_thenSavesOnConfirmation() {
        val vm = viewModel()
        vm.onMileageChanged("39000")
        vm.save()

        assertThat(vm.uiState.value.lowerMileageWarning).isEqualTo(Kilometers(40_260))
        assertThat(maintenance.recordedServices).isEmpty()

        vm.confirmLowerMileage()

        assertThat(vm.uiState.value.savedResult).isNotNull()
        assertThat(maintenance.recordedServices.single().first.mileage).isEqualTo(Kilometers(39_000))
    }

    @Test
    fun restoresDraft_afterRecreation() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            onDateSelected(LocalDate.of(2026, 9, 20))
            onMileageChanged("42500")
            onShopNameChanged("Shop")
        }

        val restored = viewModel(handle).uiState.value

        assertThat(restored.date).isEqualTo(LocalDate.of(2026, 9, 20))
        assertThat(restored.mileage).isEqualTo("42500")
        assertThat(restored.shopName).isEqualTo("Shop")
    }
}
