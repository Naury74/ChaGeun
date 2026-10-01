package com.naury.chageun.core.domain.vehicle

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.HistoryEntryError
import com.naury.chageun.core.domain.reminder.InspectionReminderStage
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeReminderNotifier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CompleteInspectionUseCaseTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val vehicleId = VehicleId("v1")
    private val history = FakeHistoryRepository()
    private val inspections = FakeInspectionRepository(InspectionSchedule(today.plusDays(10), InspectionSource.User))
    private val notifier = FakeReminderNotifier()
    private val complete = CompleteInspectionUseCase(
        AddHistoryRecordUseCase(
            history,
            FakeMaintenanceRepository(),
            Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC),
        ),
        inspections,
        notifier,
    )

    @Test
    fun recordsInspection_setsNextDate_andCancelsNotification() = runTest {
        inspections.notified = InspectionReminderStage.Days30
        val next = LocalDate.of(2028, 10, 11)

        val errors = complete(vehicleId, today, "Periodic inspection", Kilometers(42_000), next)

        assertThat(errors).isEmpty()
        val (entry, _) = history.addedChecks.single()
        assertThat(entry.kind).isEqualTo(CheckKind.Inspection)
        assertThat(entry.mileage).isEqualTo(Kilometers(42_000))
        assertThat(inspections.schedule.value).isEqualTo(InspectionSchedule(next, InspectionSource.User))
        assertThat(inspections.notified).isNull()
        assertThat(notifier.inspectionCancelCount).isEqualTo(1)
    }

    @Test
    fun futureCompletion_isRejected_andKeepsSchedule() = runTest {
        val errors = complete(vehicleId, today.plusDays(1), "Periodic inspection", null, today.plusYears(2))

        assertThat(errors).containsExactly(HistoryEntryError.FutureDate)
        assertThat(inspections.schedule.value?.nextDueDate).isEqualTo(today.plusDays(10))
        assertThat(notifier.inspectionCancelCount).isEqualTo(0)
    }

    @Test
    fun suggestion_keepsCycle_whenTakenAroundDueDate() {
        val due = LocalDate.of(2026, 10, 20)

        assertThat(CompleteInspectionUseCase.suggestNextDueDate(today, due)).isEqualTo(LocalDate.of(2028, 10, 20))
    }

    @Test
    fun suggestion_countsFromInspectionDay_whenFarFromDueDate_orUnknown() {
        assertThat(CompleteInspectionUseCase.suggestNextDueDate(today, LocalDate.of(2026, 6, 1)))
            .isEqualTo(LocalDate.of(2028, 10, 1))
        assertThat(CompleteInspectionUseCase.suggestNextDueDate(today, null)).isEqualTo(LocalDate.of(2028, 10, 1))
    }
}
