package com.naury.chageun.core.domain.history

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AddHistoryRecordUseCaseTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val vehicleId = VehicleId("v1")
    private val history = FakeHistoryRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val addRecord = AddHistoryRecordUseCase(
        history,
        maintenance,
        Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC),
    )

    @Before
    fun setUp() {
        maintenance.inputs.value = MaintenanceInputs(
            rules = emptyList(),
            lastServices = emptyMap(),
            mileageHistory = listOf(MileageReading(LocalDate.of(2026, 9, 1), Kilometers(42_000))),
        )
    }

    private fun fuel(km: Long, date: LocalDate = today) =
        FuelEntry(date, Kilometers(km), FuelAmounts(70_000, 41_176, 1_700, null), isFullTank = true)

    @Test
    fun advancesOdometer_onlyForHigherMileage() = runTest {
        addRecord.addFuel(vehicleId, fuel(43_000))
        addRecord.addFuel(vehicleId, fuel(41_000))

        assertThat(history.addedFuel.map { it.second }).containsExactly(true, false).inOrder()
    }

    @Test
    fun rejectsFutureDate() = runTest {
        assertThat(
            addRecord.addFuel(vehicleId, fuel(43_000, today.plusDays(1))),
        ).containsExactly(HistoryEntryError.FutureDate)
        assertThat(history.addedFuel).isEmpty()
    }

    @Test
    fun requiresTitle_andTrimsIt_forChecks() = runTest {
        val blank = CheckEntry(CheckKind.Repair, today, "  ")
        val valid = CheckEntry(CheckKind.Repair, today, "  Bumper  ", costWon = 0)

        assertThat(addRecord.addCheck(vehicleId, blank)).containsExactly(HistoryEntryError.MissingTitle)
        assertThat(addRecord.addCheck(vehicleId, valid)).isEmpty()
        assertThat(history.addedChecks.single().first.title).isEqualTo("Bumper")
        assertThat(history.addedChecks.single().second).isFalse()
    }
}
