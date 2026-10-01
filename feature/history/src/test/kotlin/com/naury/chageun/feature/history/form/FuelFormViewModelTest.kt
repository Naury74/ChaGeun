package com.naury.chageun.feature.history.form

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
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

class FuelFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val history = FakeHistoryRepository()

    private fun viewModel() = FuelFormViewModel(
        SavedStateHandle(),
        vehicles,
        maintenance,
        AddHistoryRecordUseCase(history, maintenance, clock),
        clock,
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(42_000)))
        maintenance.inputs.value = MaintenanceInputs(
            rules = emptyList(),
            lastServices = emptyMap(),
            mileageHistory = listOf(MileageReading(LocalDate.of(2026, 9, 1), Kilometers(42_000))),
        )
    }

    @Test
    fun parsesLitres_andPreviewsComputedTotal() {
        val vm = viewModel()
        vm.onVolumeChanged("40.25")
        vm.onUnitPriceChanged("1,689")

        assertThat(vm.uiState.value.amounts).isEqualTo(FuelAmounts(67_982, 40_250, 1_689, FuelField.Total))
    }

    @Test
    fun savesWithCurrentMileageDefault() {
        val vm = viewModel()
        vm.onTotalChanged("70000")
        vm.onUnitPriceChanged("1700")
        vm.save()

        val (entry, advances) = history.addedFuel.single()
        assertThat(vm.uiState.value.isSaved).isTrue()
        assertThat(entry.mileage).isEqualTo(Kilometers(42_000))
        assertThat(entry.amounts.volumeMl).isEqualTo(41_176)
        assertThat(advances).isFalse()
    }

    @Test
    fun requiresTwoAmounts() {
        val vm = viewModel()
        vm.onTotalChanged("70000")
        vm.save()

        assertThat(vm.uiState.value.errors).containsExactly(FormError.Amounts)
        assertThat(history.addedFuel).isEmpty()
    }
}
