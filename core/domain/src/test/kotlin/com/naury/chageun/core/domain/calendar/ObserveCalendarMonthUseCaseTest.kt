package com.naury.chageun.core.domain.calendar

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.testing.FakeAlbumRepository
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveCalendarMonthUseCaseTest {

    private val today = LocalDate.of(2026, 10, 12)
    private val clock = Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZoneOffset.UTC)
    private val october = YearMonth.of(2026, 10)
    private val history = FakeHistoryRepository()
    private val album = FakeAlbumRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val inspections = FakeInspectionRepository()
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
    private val useCase = ObserveCalendarMonthUseCase(history, album, upcoming, clock)

    private fun record(id: String, type: TimelineEventType, date: LocalDate?, cost: Long? = null) =
        TimelineItem(RecordRef(type, id), date, null, null, Kilometers(40_000), cost, RecordSource.User, Instant.EPOCH)

    private fun photo(id: String, date: LocalDate, createdAt: Instant = Instant.EPOCH) =
        AlbumPhoto(id, "/$id.jpg", "/${id}_thumb.jpg", date, null, createdAt)

    private suspend fun month(firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY) =
        useCase(VehicleId("v1"), october, firstDayOfWeek).first()

    @Test
    fun gridRange_followsTheFirstDayOfWeek() {
        // 2026년 10월 1일은 목요일, 31일은 토요일이다.
        assertThat(ObserveCalendarMonthUseCase.gridRange(october, DayOfWeek.SUNDAY))
            .isEqualTo(LocalDate.of(2026, 9, 27) to LocalDate.of(2026, 10, 31))
        assertThat(ObserveCalendarMonthUseCase.gridRange(october, DayOfWeek.MONDAY))
            .isEqualTo(LocalDate.of(2026, 9, 28) to LocalDate.of(2026, 11, 1))
    }

    @Test
    fun firstDayOfWeek_comesFromTheLocale() {
        assertThat(WeekFields.of(Locale.KOREA).firstDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(WeekFields.of(Locale.GERMANY).firstDayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(WeekFields.of(Locale.FRANCE).firstDayOfWeek).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun weeks_coverTheGridInRowsOfSeven() = runTest {
        val weeks = month(DayOfWeek.MONDAY).weeks

        assertThat(weeks).hasSize(5)
        assertThat(weeks.all { it.size == 7 }).isTrue()
        assertThat(weeks.first().first()).isEqualTo(LocalDate.of(2026, 9, 28))
        assertThat(weeks.last().last()).isEqualTo(LocalDate.of(2026, 11, 1))
    }

    @Test
    fun layersRecordsPhotosAndUpcoming_byDate_includingAdjacentMonthDays() = runTest {
        history.timeline.value = listOf(
            record("fuel", TimelineEventType.Fuel, LocalDate.of(2026, 10, 3), cost = 70_000),
            record("mileage", TimelineEventType.Mileage, LocalDate.of(2026, 10, 3)),
            record("prev", TimelineEventType.Note, LocalDate.of(2026, 9, 28), cost = 5_000),
            record("outside", TimelineEventType.Repair, LocalDate.of(2026, 9, 26), cost = 9_000),
            record("undated", TimelineEventType.Maintenance, null, cost = 1_000),
        )
        album.photos.value = listOf(
            photo("late", LocalDate.of(2026, 10, 3), Instant.EPOCH.plusSeconds(60)),
            photo("early", LocalDate.of(2026, 10, 3)),
            photo("next-month", LocalDate.of(2026, 11, 1)),
        )
        inspections.schedule.value = InspectionSchedule(LocalDate.of(2026, 10, 30), InspectionSource.User)
        maintenance.inputs.value =
            MaintenanceInputs(emptyList(), emptyMap(), listOf(MileageReading(today, Kilometers(1))))

        val calendar = month()

        val busy = calendar.day(LocalDate.of(2026, 10, 3))
        assertThat(busy.records.map { it.ref.id }).containsExactly("fuel", "mileage")
        assertThat(busy.photos.map { it.id }).containsExactly("early", "late").inOrder()
        assertThat(calendar.day(LocalDate.of(2026, 9, 28)).records.map { it.ref.id }).containsExactly("prev")
        assertThat(calendar.days.keys).doesNotContain(LocalDate.of(2026, 9, 26))
        assertThat(calendar.day(LocalDate.of(2026, 10, 30)).upcoming)
            .containsExactly(UpcomingEvent.InspectionDue(LocalDate.of(2026, 10, 30)))
        // 다음 달 날짜는 Monday 시작 그리드에서만 보인다.
        assertThat(calendar.days.keys).doesNotContain(LocalDate.of(2026, 11, 1))
        assertThat(month(DayOfWeek.MONDAY).day(LocalDate.of(2026, 11, 1)).photos).hasSize(1)
    }

    @Test
    fun monthSummary_countsOnlyTheMonthItself() = runTest {
        history.timeline.value = listOf(
            record("oct-1", TimelineEventType.Fuel, LocalDate.of(2026, 10, 3), cost = 70_000),
            record("oct-2", TimelineEventType.Maintenance, LocalDate.of(2026, 10, 20), cost = 95_000),
            record("oct-3", TimelineEventType.Mileage, LocalDate.of(2026, 10, 21)),
            record("sep", TimelineEventType.Note, LocalDate.of(2026, 9, 28), cost = 5_000),
        )
        album.photos.value = listOf(photo("oct", LocalDate.of(2026, 10, 3)), photo("sep", LocalDate.of(2026, 9, 27)))

        val calendar = month()

        assertThat(calendar.monthTotalWon).isEqualTo(165_000)
        assertThat(calendar.recordCount).isEqualTo(3)
        assertThat(calendar.photoCount).isEqualTo(1)
        assertThat(calendar.today).isEqualTo(today)
    }

    @Test
    fun overdueMaintenance_appearsOnlyOnToday() = runTest {
        maintenance.inputs.value = MaintenanceInputs(
            rules = listOf(MaintenanceRule(MaintenanceItem.Wiper, intervalKm = null, intervalMonths = 12)),
            lastServices = mapOf(MaintenanceItem.Wiper to ServiceRecord(LocalDate.of(2025, 9, 1), null)),
            mileageHistory = listOf(MileageReading(today, Kilometers(40_000))),
        )

        val calendar = month()

        val withMaintenance = calendar.days.filterValues { day ->
            day.upcoming.any { it is UpcomingEvent.MaintenanceDue }
        }
        assertThat(withMaintenance.keys).containsExactly(today)
        assertThat(withMaintenance.getValue(today).upcoming.first().isOverdue).isTrue()
    }

    @Test
    fun upcomingEvents_areLimitedToTheRange() = runTest {
        inspections.schedule.value = InspectionSchedule(today.plusDays(20), InspectionSource.User)
        maintenance.inputs.value =
            MaintenanceInputs(emptyList(), emptyMap(), listOf(MileageReading(today, Kilometers(1))))

        val soon = upcoming(VehicleId("v1"), today, today.plusDays(14)).first()
        val month = upcoming(VehicleId("v1"), today, today.plusDays(30)).first()

        assertThat(soon).isEmpty()
        assertThat(month).containsExactly(
            UpcomingEvent.InspectionDue(today.plusDays(20)),
            UpcomingEvent.MileageUpdate(today.plusDays(30)),
        ).inOrder()
    }
}
