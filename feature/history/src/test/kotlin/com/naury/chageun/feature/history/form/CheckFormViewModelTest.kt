package com.naury.chageun.feature.history.form

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CheckFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val vehicles = FakeVehicleRepository()
    private val history = FakeHistoryRepository()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = CheckFormViewModel(
        handle,
        vehicles,
        AddHistoryRecordUseCase(history, FakeMaintenanceRepository(), clock),
        clock,
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(42_000)))
    }

    @Test
    fun requiresTitle() {
        val vm = viewModel()
        vm.save()

        assertThat(vm.uiState.value.errors).containsExactly(FormError.Required)
        assertThat(vm.uiState.value.isSaved).isFalse()
    }

    @Test
    fun savesRepairWithCost() {
        val vm = viewModel()
        vm.onKindSelected(CheckKind.Repair)
        vm.onTitleChanged("Bumper")
        vm.onCostChanged("120,000")
        vm.save()

        val entry = history.addedChecks.single().first
        assertThat(entry.kind).isEqualTo(CheckKind.Repair)
        assertThat(entry.costWon).isEqualTo(120_000)
        assertThat(vm.uiState.value.isSaved).isTrue()
    }

    @Test
    fun restoresDraft() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            onKindSelected(CheckKind.Note)
            onTitleChanged("Car wash")
        }

        val restored = viewModel(handle).uiState.value

        assertThat(restored.kind).isEqualTo(CheckKind.Note)
        assertThat(restored.title).isEqualTo("Car wash")
    }
}
