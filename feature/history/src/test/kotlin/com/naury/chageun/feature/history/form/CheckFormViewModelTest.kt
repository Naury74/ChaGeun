package com.naury.chageun.feature.history.form

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.EditHistoryRecordUseCase
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.PeriodicInspectionResult
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
        EditHistoryRecordUseCase(history, clock),
        FakeAnalyticsTracker(),
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

    @Test
    fun editing_updatesSameRecord() = runTest {
        val ref = RecordRef(TimelineEventType.Repair, "c1")
        val saved = CheckEntry(CheckKind.Repair, LocalDate.of(2026, 9, 1), "Bumper", Kilometers(41_000), 120_000)
        history.details.value = mapOf(ref to RecordDetail.Check(ref, saved))
        val vm = viewModel()

        vm.startEditing(ref)
        assertThat(vm.uiState.value.title).isEqualTo("Bumper")
        vm.onCostChanged("150000")
        vm.save()

        val (id, entry) = history.updated.single()
        assertThat(id).isEqualTo("c1")
        assertThat((entry as CheckEntry).costWon).isEqualTo(150_000)
        assertThat(entry.kind).isEqualTo(CheckKind.Repair)
        assertThat(entry.periodicResult).isNull()
        assertThat(history.addedChecks).isEmpty()
    }

    @Test
    fun editingPeriodicInspection_keepsResult_andCanCorrectIt() = runTest {
        val ref = RecordRef(TimelineEventType.Inspection, "c2")
        val saved = CheckEntry(
            kind = CheckKind.Inspection,
            date = LocalDate.of(2026, 9, 1),
            title = "Periodic inspection",
            mileage = Kilometers(41_000),
            periodicResult = PeriodicInspectionResult.Unknown,
        )
        history.details.value = mapOf(ref to RecordDetail.Check(ref, saved))
        val vm = viewModel()

        vm.startEditing(ref)
        assertThat(vm.uiState.value.isPeriodicInspection).isTrue()
        vm.onMileageChanged("41500")
        vm.save()
        assertThat((history.updated.last().second as CheckEntry).periodicResult)
            .isEqualTo(PeriodicInspectionResult.Unknown)

        vm.onPeriodicResultSelected(PeriodicInspectionResult.Failed)
        vm.save()
        assertThat((history.updated.last().second as CheckEntry).periodicResult)
            .isEqualTo(PeriodicInspectionResult.Failed)
    }

    @Test
    fun newInspection_isGeneralCheck() {
        val vm = viewModel()
        vm.onTitleChanged("Free check")
        vm.save()

        assertThat(vm.uiState.value.isPeriodicInspection).isFalse()
        assertThat(history.addedChecks.single().first.periodicResult).isNull()
    }
}
