package com.naury.chageun.core.domain.history

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.testing.FakeHistoryRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EditHistoryRecordUseCaseTest {

    private val vehicleId = VehicleId("v1")
    private val today = LocalDate.of(2026, 10, 1)
    private val history = FakeHistoryRepository()
    private val edit =
        EditHistoryRecordUseCase(history, Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC))

    @Test
    fun updatesCheck_withTrimmedTitle() = runTest {
        val errors = edit.updateCheck(vehicleId, "c1", CheckEntry(CheckKind.Note, today, "  Car wash "))

        assertThat(errors).isEmpty()
        assertThat((history.updated.single().second as CheckEntry).title).isEqualTo("Car wash")
    }

    @Test
    fun rejectsFutureDateBlankTitleAndNegativeCost_withoutSaving() = runTest {
        val errors = edit.updateCheck(
            vehicleId,
            "c1",
            CheckEntry(CheckKind.Repair, today.plusDays(1), " ", costWon = -1),
        )

        assertThat(errors).containsExactly(
            HistoryEntryError.FutureDate,
            HistoryEntryError.MissingTitle,
            HistoryEntryError.NegativeCost,
        )
        assertThat(history.updated).isEmpty()
    }

    @Test
    fun updatesService_andRejectsNegativeCost() = runTest {
        val entry = ServiceEntry(MaintenanceItem.Tire, today, Kilometers(40_000), costWon = 300_000)

        assertThat(edit.updateService(vehicleId, "r1", entry)).isEmpty()
        assertThat(
            edit.updateService(vehicleId, "r1", entry.copy(costWon = -5)),
        ).containsExactly(HistoryEntryError.NegativeCost)
        assertThat(history.updated.map { it.first }).containsExactly("r1")
    }
}
