package com.naury.chageun.feature.history

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository()
    private val history = FakeHistoryRepository()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = HistoryViewModel(handle, vehicles, history)

    private fun item(type: TimelineEventType, id: String, date: LocalDate?) = TimelineItem(
        ref = RecordRef(type, id),
        date = date,
        title = id,
        maintenanceItem = MaintenanceItem.EngineOil.takeIf { type == TimelineEventType.Maintenance },
        mileage = Kilometers(40_000),
        costWon = 10_000,
        source = RecordSource.User,
        createdAt = Instant.EPOCH,
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(40_000)))
        history.timeline.value = listOf(
            item(TimelineEventType.Fuel, "fuel", LocalDate.of(2026, 9, 20)),
            item(TimelineEventType.Repair, "repair", LocalDate.of(2026, 9, 1)),
            item(TimelineEventType.Maintenance, "oil", LocalDate.of(2026, 3, 10)),
            item(TimelineEventType.Maintenance, "tire", null),
        )
    }

    @Test
    fun groupsByMonth_keepingUndatedLast() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        val sections = vm.uiState.first { !it.isLoading }.sections

        assertThat(
            sections.map {
                it.month
            },
        ).containsExactly(YearMonth.of(2026, 9), YearMonth.of(2026, 3), null).inOrder()
        assertThat(sections.first().items.map { it.ref.id }).containsExactly("fuel", "repair").inOrder()
    }

    @Test
    fun filterAndSearch_reachTheRepositoryQuery() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.selectFilter(HistoryFilter.Check)
        vm.search("엔진", setOf(MaintenanceItem.EngineOil))
        val state = vm.uiState.first { it.filter == HistoryFilter.Check && it.keyword == "엔진" }

        assertThat(state.sections.flatMap { it.items }.map { it.ref.id }).containsExactly("repair")
        assertThat(history.lastQuery?.types).containsExactly(TimelineEventType.Inspection, TimelineEventType.Repair)
        assertThat(history.lastQuery?.matchingItems).containsExactly(MaintenanceItem.EngineOil)
    }

    @Test
    fun showsDetail_andClearsSelectionAfterDelete() = runTest {
        val ref = RecordRef(TimelineEventType.Repair, "repair")
        history.details.value =
            mapOf(ref to RecordDetail.Check(ref, CheckEntry(CheckKind.Repair, LocalDate.of(2026, 9, 1), "Bumper")))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.select(ref)
        assertThat(vm.uiState.first { it.detail != null }.selected).isEqualTo(ref)

        vm.delete(ref)

        assertThat(history.deleted).containsExactly(ref)
        assertThat(vm.uiState.first { it.detail == null }.sections.flatMap { it.items }.map { it.ref.id })
            .doesNotContain("repair")
    }

    @Test
    fun keepsFilterAndSelection_acrossRecreation() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            selectFilter(HistoryFilter.Fuel)
            select(RecordRef(TimelineEventType.Fuel, "fuel"))
        }

        val restored = viewModel(handle)
        backgroundScope.launch { restored.uiState.collect {} }

        assertThat(restored.uiState.first { !it.isLoading }.filter).isEqualTo(HistoryFilter.Fuel)
    }
}
