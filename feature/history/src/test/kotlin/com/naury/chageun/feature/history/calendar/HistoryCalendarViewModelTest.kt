package com.naury.chageun.feature.history.calendar

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.calendar.ObserveCalendarMonthUseCase
import com.naury.chageun.core.domain.calendar.ObserveUpcomingEventsUseCase
import com.naury.chageun.core.domain.calendar.SearchCalendarRecordsUseCase
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.HistoryView
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAlbumRepository
import com.naury.chageun.core.testing.FakeAttachmentRepository
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
    private val attachments = FakeAttachmentRepository()
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
        CalendarDataSources(
            ObserveCalendarMonthUseCase(history, album, attachments, upcoming, clock),
            upcoming,
            SearchCalendarRecordsUseCase(history),
        ),
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

        vm.navigation.showMonth(YearMonth.of(2026, 11))
        val november = vm.uiState.first { it.calendar?.month == YearMonth.of(2026, 11) }
        assertThat(november.selectedDate).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(november.calendar?.monthTotalWon).isEqualTo(9_000)

        vm.navigation.showToday()
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

        vm.navigation.selectDate(today.plusDays(1))
        advanceUntilIdle()
        assertThat(vm.uiState.value.upcomingSoon).isEmpty()
    }

    @Test
    fun selectionModeFiltersAndSearch_surviveRecreation() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.navigation.selectDate(LocalDate.of(2026, 10, 3))
        vm.navigation.setMode(CalendarMode.Week)
        vm.query.toggleKind(CalendarKind.Fuel)
        vm.query.setItems(setOf(MaintenanceItem.Tire))
        vm.query.openSearch()
        vm.query.search("fuel", emptySet())
        advanceUntilIdle()

        val restored = viewModel(handle)
        backgroundScope.launch { restored.uiState.collect {} }
        val state = restored.uiState.first { !it.isLoading }
        assertThat(state.selectedDate).isEqualTo(LocalDate.of(2026, 10, 3))
        assertThat(state.mode).isEqualTo(CalendarMode.Week)
        assertThat(state.filter).isEqualTo(CalendarFilter(setOf(CalendarKind.Fuel), setOf(MaintenanceItem.Tire)))
        assertThat(state.search.isOpen).isTrue()
        assertThat(state.search.keyword).isEqualTo("fuel")
    }

    @Test
    fun modeIsUnsetUntilChosen_soTheLayoutPicksItsDefault() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        assertThat(vm.uiState.first { !it.isLoading }.mode).isNull()

        vm.navigation.setMode(CalendarMode.Week)
        assertThat(vm.uiState.first { it.mode != null }.mode).isEqualTo(CalendarMode.Week)
    }

    @Test
    fun movingWeeks_keepsTheWeekday_andFollowsIntoTheNextMonth() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.navigation.setMode(CalendarMode.Week)

        vm.navigation.moveWeek(1)
        val sameMonth = vm.uiState.first { it.selectedDate == LocalDate.of(2026, 10, 19) }
        assertThat(sameMonth.calendar?.month).isEqualTo(YearMonth.of(2026, 10))
        assertThat(sameMonth.selectedWeek(checkNotNull(sameMonth.calendar)))
            .containsExactlyElementsIn((18..24).map { LocalDate.of(2026, 10, it) }).inOrder()

        vm.navigation.moveWeek(2)
        val november = vm.uiState.first { it.calendar?.month == YearMonth.of(2026, 11) }
        assertThat(november.selectedDate).isEqualTo(LocalDate.of(2026, 11, 2))
        assertThat(november.visibleDay(LocalDate.of(2026, 11, 20)).records.map { it.ref.id }).containsExactly("nov")

        vm.navigation.moveWeek(-3)
        val back = vm.uiState.first { it.selectedDate == LocalDate.of(2026, 10, 12) }
        assertThat(back.calendar?.month).isEqualTo(YearMonth.of(2026, 10))
    }

    @Test
    fun filters_hideOtherKinds_andTheMonthSummaryCountsOnlyWhatIsShown() = runTest {
        history.timeline.value = history.timeline.value + listOf(
            CalendarFixtures.maintenance("oil", LocalDate.of(2026, 10, 3), MaintenanceItem.EngineOil)
                .copy(costWon = 95_000),
            CalendarFixtures.maintenance("tire", LocalDate.of(2026, 10, 5), MaintenanceItem.Tire)
                .copy(costWon = 400_000),
        )
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.query.toggleKind(CalendarKind.Fuel)
        val fuelOnly = vm.uiState.first { it.filter.kinds.isNotEmpty() && it.calendar != null }
        val calendar = checkNotNull(fuelOnly.calendar)
        assertThat(fuelOnly.visibleDay(LocalDate.of(2026, 10, 3)).records.map { it.ref.id }).containsExactly("fuel")
        assertThat(fuelOnly.monthSummary(calendar)).isEqualTo(MonthSummaryValues(70_000, 1, 0))

        vm.query.toggleKind(CalendarKind.Fuel)
        vm.query.setItems(setOf(MaintenanceItem.EngineOil))
        val oilOnly = vm.uiState.first { it.filter.items.isNotEmpty() }
        assertThat(oilOnly.visibleDay(LocalDate.of(2026, 10, 3)).records.map { it.ref.id }).containsExactly("oil")
        assertThat(oilOnly.monthSummary(calendar)).isEqualTo(MonthSummaryValues(95_000, 1, 0))
        assertThat(oilOnly.filter.activeCount).isEqualTo(1)

        vm.query.clearAll()
        val cleared = vm.uiState.first { !it.filter.isActive }
        assertThat(cleared.monthSummary(calendar).recordCount).isEqualTo(3)
    }

    @Test
    fun recordPhotos_joinTheCalendar_andHideWithThePhotoFilterOff() = runTest {
        val fuel = RecordRef(TimelineEventType.Fuel, "fuel")
        attachments.attachments.value = listOf(Attachment("receipt", fuel, "/r.jpg", "/r_thumb.jpg", Instant.EPOCH))
        attachments.recordDates.value = mapOf(fuel to LocalDate.of(2026, 10, 3))
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        val state = vm.uiState.first { it.calendar?.photoCount == 1 }
        assertThat(state.visibleDay(LocalDate.of(2026, 10, 3)).recordPhotos.single().attachment.owner).isEqualTo(fuel)

        vm.query.toggleKind(CalendarKind.Fuel)
        val fuelOnly = vm.uiState.first { it.filter.isActive }
        assertThat(fuelOnly.visibleDay(LocalDate.of(2026, 10, 3)).photoCount).isEqualTo(0)
    }

    @Test
    fun search_findsAcrossMonths_andStepsBetweenResultDates() = runTest {
        history.appliesKeyword = true
        history.timeline.value = listOf(
            CalendarFixtures.maintenance("oil-nov", LocalDate.of(2026, 11, 20), MaintenanceItem.EngineOil),
            CalendarFixtures.maintenance("oil-oct", LocalDate.of(2026, 10, 8), MaintenanceItem.EngineOil),
            CalendarFixtures.record("note", TimelineEventType.Note, LocalDate.of(2026, 8, 1), title = "Oil leak"),
            CalendarFixtures.record("wash", TimelineEventType.Note, LocalDate.of(2026, 10, 9), title = "Car wash"),
        )
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.query.openSearch()
        vm.query.search("oil", setOf(MaintenanceItem.EngineOil))
        val found = vm.uiState.first { it.searchResults.isNotEmpty() }
        assertThat(found.searchResults.map { it.ref.id }).containsExactly("oil-nov", "oil-oct", "note")
        assertThat(found.resultDates).containsExactly(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 10, 8),
            LocalDate.of(2026, 11, 20),
        ).inOrder()

        // 오늘(10월 12일)에서 다음 결과는 11월 20일이다.
        vm.showNextResult()
        val next = vm.uiState.first { it.calendar?.month == YearMonth.of(2026, 11) }
        assertThat(next.selectedDate).isEqualTo(LocalDate.of(2026, 11, 20))
        assertThat(next.nextResultDate).isNull()

        vm.showPreviousResult()
        vm.uiState.first { it.selectedDate == LocalDate.of(2026, 10, 8) }
        vm.showPreviousResult()
        val august = vm.uiState.first { it.calendar?.month == YearMonth.of(2026, 8) }
        assertThat(august.selectedDate).isEqualTo(LocalDate.of(2026, 8, 1))
        assertThat(august.previousResultDate).isNull()

        vm.openResult(found.searchResults.first { it.ref.id == "oil-oct" })
        assertThat(vm.uiState.first { it.selectedDate == LocalDate.of(2026, 10, 8) }.calendar).isNotNull()

        vm.query.closeSearch()
        val closed = vm.uiState.first { !it.search.isOpen }
        assertThat(closed.search.isActive).isFalse()
        assertThat(closed.searchResults).isEmpty()
    }

    @Test
    fun searchResults_followTheFilters() = runTest {
        history.appliesKeyword = true
        history.timeline.value = listOf(
            CalendarFixtures.maintenance("oil", LocalDate.of(2026, 10, 8), MaintenanceItem.EngineOil),
            CalendarFixtures.record("note", TimelineEventType.Note, LocalDate.of(2026, 8, 1), title = "Oil leak"),
        )
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.query.search("oil", setOf(MaintenanceItem.EngineOil))
        vm.query.toggleKind(CalendarKind.Maintenance)

        val state = vm.uiState.first { it.filter.isActive && it.search.results.isNotEmpty() }
        assertThat(state.searchResults.map { it.ref.id }).containsExactly("oil")
        assertThat(state.isFilteredOrSearching).isTrue()
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
