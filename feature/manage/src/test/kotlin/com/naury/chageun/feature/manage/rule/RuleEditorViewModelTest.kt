package com.naury.chageun.feature.manage.rule

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.EditMaintenanceRuleUseCase
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.RuleEditError
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuleEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = RuleEditorViewModel(
        item = MaintenanceItem.EngineOil,
        savedStateHandle = handle,
        vehicleRepository = vehicles,
        maintenanceRepository = maintenance,
        editRule = EditMaintenanceRuleUseCase(maintenance, vehicles),
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(10_000)))
        maintenance.inputs.value = MaintenanceInputs(
            rules = listOf(MaintenanceRule(MaintenanceItem.EngineOil, 10_000, 12)),
            lastServices = emptyMap(),
            mileageHistory = emptyList(),
        )
    }

    @Test
    fun loadsCurrentRule() {
        val state = viewModel().uiState.value

        assertThat(state.intervalKm).isEqualTo("10000")
        assertThat(state.intervalMonths).isEqualTo("12")
        assertThat(state.source).isEqualTo(RuleSource.Generic)
    }

    @Test
    fun savesEditedRule_andFinishes() {
        val vm = viewModel()
        vm.onIntervalKmChanged("8,000")
        vm.onIntervalMonthsChanged("")
        vm.save()

        assertThat(vm.uiState.value.isDone).isTrue()
        assertThat(maintenance.inputs.value.rules.single())
            .isEqualTo(MaintenanceRule(MaintenanceItem.EngineOil, 8_000, null, source = RuleSource.User))
    }

    @Test
    fun showsError_whenBothIntervalsAreEmpty() {
        val vm = viewModel()
        vm.onIntervalKmChanged("")
        vm.onIntervalMonthsChanged("")
        vm.save()

        assertThat(vm.uiState.value.error).isEqualTo(RuleEditError.NoInterval)
        assertThat(vm.uiState.value.isDone).isFalse()
    }

    @Test
    fun restoresDraft_afterRecreation() {
        val handle = SavedStateHandle()
        viewModel(handle).onIntervalKmChanged("5000")

        assertThat(viewModel(handle).uiState.value.intervalKm).isEqualTo("5000")
    }
}
