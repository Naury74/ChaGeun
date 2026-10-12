package com.naury.chageun.feature.history.calendar

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.calendar.ObserveCalendarMonthUseCase
import com.naury.chageun.core.domain.calendar.ObserveUpcomingEventsUseCase
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.HistoryView
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAlbumRepository
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeSettingsRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryCalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 12)
    private val clock = Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZoneOffset.UTC)
    private val vehicles = FakeVehicleRepository()
    private val history = FakeHistoryRepository()
    private val album = FakeAlbumRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val inspections = FakeInspectionRepository()
    private val settings = FakeSettingsRepository()
    private val upcoming = ObserveUpcomingEventsUseCase(
        ObserveMaintenanceOverviewUseCase(
            maintenance,
            inspections,
            RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
            VehicleHealthAggregator(),
            clock,
        ),
        clock,
    )

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = HistoryCalendarViewModel(
        handle,
        vehicles,
        ObserveCalendarMonthUseCase(history, album, upcoming, clock),
        upcoming,
        settings,
        clock,
    ).also { it.setFirstDayOfWeek(DayOfWeek.SUNDAY) }

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(40_000)))
        maintenance.inputs.value =
            MaintenanceInputs(emptyList(), emptyMap(), listOf(MileageReading(today, Kilometers(1))))
        history.timeline.value = listOf(
            CalendarFixtures.record("fuel", TimelineEventType.Fuel, LocalDate.of(2026, 10, 3), cost = 70_000),
            CalendarFixtures.record("nov", TimelineEventType.Note, LocalDate.of(2026, 11, 20), cost = 9_000),
        )
        settings.settings.value = settings.settings.value.copy(historyView = HistoryView.Calendar)
    }

    @Test
    fun startsOnThisMonth_withTodaySelected() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        val state = vm.uiState.first { !it.isLoading }

        assertThat(state.calendar?.month).isEqualTo(YearMonth.of(2026, 10))
        assertThat(state.selectedDate).isEqualTo(today)
        assertThat(state.calendar?.monthTotalWon).isEqualTo(70_000)
        assertThat(state.isTodaySelected).isTrue()
    }

    @Test
    fun listView_doesNotReadCalendarData() = runTest {
        settings.settings.value = settings.settings.value.copy(historyView = HistoryView.List)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoading).isTrue()
        assertThat(history.lastQuery).isNull()
    }

    @Test
    fun movingMonths_selectsTheFirstDay_orTodayInThisMonth() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.showMonth(YearMonth.of(2026, 11))
        val november = vm.uiState.first { it.calendar?.month == YearMonth.of(2026, 11) }
        assertThat(november.selectedDate).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(november.calendar?.monthTotalWon).isEqualTo(9_000)

        vm.showToday()
        val back = vm.uiState.first { it.calendar?.month == YearMonth.of(2026, 10) }
        assertThat(back.selectedDate).isEqualTo(today)
    }

    @Test
    fun todaysAgenda_includesTheNextThirtyDays_otherDaysDoNot() = runTest {
        inspections.schedule.value = InspectionSchedule(today.plusDays(20), InspectionSource.User)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        val onToday = vm.uiState.first { !it.isLoading && it.upcomingSoon.isNotEmpty() }
        assertThat(onToday.upcomingSoon).contains(UpcomingEvent.InspectionDue(today.plusDays(20)))

        vm.selectDate(today.plusDays(1))
        advanceUntilIdle()
        assertThat(vm.uiState.value.upcomingSoon).isEmpty()
    }

    @Test
    fun layersAndSelection_surviveRecreation() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.selectDate(LocalDate.of(2026, 10, 3))
        vm.toggleLayer(CalendarLayer.Photos)
        advanceUntilIdle()

        val restored = viewModel(handle)
        backgroundScope.launch { restored.uiState.collect {} }
        val state = restored.uiState.first { !it.isLoading }
        assertThat(state.selectedDate).isEqualTo(LocalDate.of(2026, 10, 3))
        assertThat(state.layers).containsExactly(CalendarLayer.Records, CalendarLayer.Upcoming)
        assertThat(state.visibleDay(LocalDate.of(2026, 10, 3)).records).hasSize(1)
    }

    @Test
    fun focusOn_opensTheCalendarOnThatDate_andRemembersTheView() = runTest {
        settings.settings.value = settings.settings.value.copy(historyView = HistoryView.List)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        backgroundScope.launch { vm.view.collect {} }

        vm.focusOn(LocalDate.of(2026, 8, 15))

        val state = vm.uiState.first { it.calendar?.month == YearMonth.of(2026, 8) }
        assertThat(state.selectedDate).isEqualTo(LocalDate.of(2026, 8, 15))
        assertThat(settings.settings.value.historyView).isEqualTo(HistoryView.Calendar)
        assertThat(vm.view.first { it != null }).isEqualTo(HistoryView.Calendar)
    }

    @Test
    fun setView_persistsTheChoice() = runTest {
        val vm = viewModel()

        vm.setView(HistoryView.List)
        advanceUntilIdle()

        assertThat(settings.settings.value.historyView).isEqualTo(HistoryView.List)
    }
}
