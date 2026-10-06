package com.naury.chageun.feature.history.form

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.EditHistoryRecordUseCase
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAnalyticsTracker
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

    private val analytics = FakeAnalyticsTracker()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = FuelFormViewModel(
        handle,
        vehicles,
        AddHistoryRecordUseCase(history, maintenance, clock),
        EditHistoryRecordUseCase(history, clock),
        analytics,
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

    @Test
    fun editing_fillsUserEnteredFields_andUpdatesSameRecord() = runTest {
        val ref = RecordRef(TimelineEventType.Fuel, "f1")
        val saved = FuelEntry(
            LocalDate.of(2026, 9, 20),
            Kilometers(41_500),
            FuelAmounts(70_000, 41_176, 1_700, FuelField.Volume),
            isFullTank = false,
            stationName = "S-Oil",
        )
        history.details.value = mapOf(ref to RecordDetail.Fuel(ref, saved))
        val handle = SavedStateHandle()
        val vm = viewModel(handle)

        vm.startEditing("f1")
        val state = vm.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(
            listOf(state.total, state.volumeLitres, state.unitPrice),
        ).containsExactly("70000", "", "1700").inOrder()
        assertThat(state.mileage).isEqualTo("41500")

        vm.onStationChanged("GS")
        // 접기·회전으로 다시 불려도 고친 값을 덮어쓰지 않는다.
        vm.startEditing("f1")
        vm.save()

        val (id, entry) = history.updated.single()
        assertThat(id).isEqualTo("f1")
        assertThat((entry as FuelEntry).stationName).isEqualTo("GS")
        assertThat(history.addedFuel).isEmpty()
        assertThat(analytics.events).isEmpty()
        assertThat(viewModel(handle).uiState.value.isEditing).isTrue()
    }
}
