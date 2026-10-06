package com.naury.chageun.core.domain.maintenance

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeReminderNotifier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class RecordServiceUseCaseTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val vehicleId = VehicleId("v1")
    private val repository = FakeMaintenanceRepository()
    private val analytics = FakeAnalyticsTracker()
    private val notifier = FakeReminderNotifier()
    private val recordService = RecordServiceUseCase(
        repository,
        Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC),
        analytics,
        notifier,
    )

    @Before
    fun setUp() {
        repository.inputs.value = MaintenanceInputs(
            rules = listOf(MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12)),
            lastServices = mapOf(
                MaintenanceItem.EngineOil to ServiceRecord(LocalDate.of(2026, 3, 10), Kilometers(40_260)),
            ),
            mileageHistory = listOf(MileageReading(LocalDate.of(2026, 9, 1), Kilometers(42_000))),
        )
    }

    private fun entry(km: Long, date: LocalDate = today, cost: Long? = null) =
        ServiceEntry(MaintenanceItem.EngineOil, date, Kilometers(km), costWon = cost)

    @Test
    fun savesAndReturnsNextDue_fromRule() = runTest {
        val result = recordService(vehicleId, entry(42_891))

        assertThat(result).isEqualTo(RecordServiceResult.Saved(Kilometers(52_891), LocalDate.of(2027, 10, 1)))
        assertThat(analytics.events).containsExactly(AnalyticsEvent.MaintenanceRecordAdded(withCost = false))
    }

    @Test
    fun clearsItemNotification_whenSaved() = runTest {
        recordService(vehicleId, entry(42_891))

        assertThat(notifier.cancelledItems).containsExactly(MaintenanceItem.EngineOil)
    }

    @Test
    fun keepsNotification_whenNotSaved() = runTest {
        recordService(vehicleId, entry(42_891, date = today.plusDays(1)))
        recordService(vehicleId, entry(40_000))

        assertThat(notifier.cancelledItems).isEmpty()
    }

    @Test
    fun advancesOdometer_onlyWhenEntryIsAheadOfCurrentMileage() = runTest {
        recordService(vehicleId, entry(42_891))
        recordService(vehicleId, entry(41_000), isLowerMileageConfirmed = true)

        assertThat(repository.recordedServices.map { it.second }).containsExactly(true, false).inOrder()
    }

    @Test
    fun acceptsTodayButRejectsFutureDate() = runTest {
        assertThat(
            recordService(vehicleId, entry(42_500, date = today)),
        ).isInstanceOf(RecordServiceResult.Saved::class.java)
        assertThat(recordService(vehicleId, entry(42_600, date = today.plusDays(1))))
            .isEqualTo(RecordServiceResult.Rejected(setOf(ServiceEntryError.FutureDate)))
    }

    @Test
    fun rejectsNegativeCost_butAcceptsZero() = runTest {
        assertThat(recordService(vehicleId, entry(42_500, cost = -1)))
            .isEqualTo(RecordServiceResult.Rejected(setOf(ServiceEntryError.NegativeCost)))
        assertThat(
            recordService(vehicleId, entry(42_500, cost = 0)),
        ).isInstanceOf(RecordServiceResult.Saved::class.java)
    }

    @Test
    fun asksForConfirmation_whenLowerThanPreviousService() = runTest {
        val result = recordService(vehicleId, entry(39_000))

        assertThat(result).isEqualTo(RecordServiceResult.NeedsConfirmation(Kilometers(40_260)))
        assertThat(repository.recordedServices).isEmpty()
    }

    @Test
    fun savesLowerMileage_afterConfirmation() = runTest {
        val result = recordService(vehicleId, entry(39_000), isLowerMileageConfirmed = true)

        assertThat(result).isInstanceOf(RecordServiceResult.Saved::class.java)
        assertThat(repository.recordedServices.single().second).isFalse()
    }

    @Test
    fun alsoReplaced_savesEachItemOnTheSameDay_withCostOnlyOnce() = runTest {
        val result = recordService(
            vehicleId,
            entry(42_891, cost = 80_000).copy(shopName = "Blue Hands", memo = "Synthetic"),
            alsoReplaced = setOf(MaintenanceItem.OilFilter, MaintenanceItem.EngineOil),
        )

        val (oil, filter) = repository.recordedServices
        assertThat(repository.recordedServices).hasSize(2)
        assertThat(oil.first.costWon).isEqualTo(80_000)
        assertThat(filter.first.item).isEqualTo(MaintenanceItem.OilFilter)
        assertThat(filter.first.date).isEqualTo(today)
        assertThat(filter.first.mileage).isEqualTo(Kilometers(42_891))
        assertThat(filter.first.shopName).isEqualTo("Blue Hands")
        assertThat(filter.first.costWon).isNull()
        assertThat(filter.first.memo).isNull()
        // 주행거리 기록은 첫 항목에서 한 번만 남긴다.
        assertThat(filter.second).isFalse()
        assertThat(notifier.cancelledItems).containsExactly(MaintenanceItem.EngineOil, MaintenanceItem.OilFilter)
        assertThat((result as RecordServiceResult.Saved).alsoReplaced).containsExactly(MaintenanceItem.OilFilter)
    }

    @Test
    fun alsoReplaced_isNotSaved_whenTheMainEntryNeedsConfirmation() = runTest {
        val result = recordService(vehicleId, entry(40_000), alsoReplaced = setOf(MaintenanceItem.OilFilter))

        assertThat(result).isInstanceOf(RecordServiceResult.NeedsConfirmation::class.java)
        assertThat(repository.recordedServices).isEmpty()
    }

    @Test
    fun companionCandidates_putUsualPartnersFirst_andSkipDisabledItems() = runTest {
        repository.inputs.value = repository.inputs.value.copy(
            rules = listOf(
                MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.AirFilter, intervalKm = 20_000, intervalMonths = 24),
                MaintenanceRule(MaintenanceItem.Wiper, intervalKm = null, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.OilFilter, intervalKm = 10_000, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.Tire, intervalKm = 50_000, intervalMonths = 48, isEnabled = false),
            ),
        )

        assertThat(recordService.companionCandidates(vehicleId, MaintenanceItem.EngineOil)).containsExactly(
            MaintenanceItem.OilFilter,
            MaintenanceItem.AirFilter,
            MaintenanceItem.Wiper,
        ).inOrder()
    }
}
