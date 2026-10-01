package com.naury.chageun.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeReminderRepository
import com.naury.chageun.core.testing.FakeSettingsRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EvaluateRemindersUseCaseTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val reminders = FakeReminderRepository()
    private val settings = FakeSettingsRepository()
    private val inspections = FakeInspectionRepository()
    private val notifier = object : ReminderNotifier {
        var enabled = true
        val shown = mutableListOf<MaintenanceStatus>()
        val inspectionShown = mutableListOf<InspectionStatus>()

        override fun canNotify() = enabled

        override fun notify(statuses: List<MaintenanceStatus>) {
            shown += statuses
        }

        override fun notifyInspection(status: InspectionStatus) {
            inspectionShown += status
        }
    }
    private val evaluate = EvaluateRemindersUseCase(
        vehicles,
        ObserveMaintenanceOverviewUseCase(
            maintenance,
            inspections,
            RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
            VehicleHealthAggregator(),
            Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC),
        ),
        reminders,
        inspections,
        notifier,
        settings,
    )

    private suspend fun givenOilDueSoon() {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(49_700)))
        maintenance.inputs.value = MaintenanceInputs(
            rules = listOf(MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12)),
            lastServices = mapOf(MaintenanceItem.EngineOil to ServiceRecord(today.minusMonths(6), Kilometers(40_000))),
            mileageHistory = listOf(MileageReading(today, Kilometers(49_700))),
        )
    }

    @Test
    fun notifiesOnce_perStage() = runTest {
        givenOilDueSoon()

        assertThat(evaluate()).isEqualTo(1)
        assertThat(evaluate()).isEqualTo(0)
        assertThat(notifier.shown.single().state).isEqualTo(MaintenanceState.Due)
        assertThat(reminders.states).containsExactly(MaintenanceItem.EngineOil, MaintenanceState.Due)
    }

    @Test
    fun recordsNothing_whenNotificationsAreBlocked() = runTest {
        givenOilDueSoon()
        notifier.enabled = false

        assertThat(evaluate()).isEqualTo(0)
        assertThat(reminders.states).isEmpty()
    }

    @Test
    fun staysSilent_whenUserTurnedRemindersOff() = runTest {
        givenOilDueSoon()
        settings.setMaintenanceReminderEnabled(false)

        assertThat(evaluate()).isEqualTo(0)
        assertThat(notifier.shown).isEmpty()
    }

    @Test
    fun doesNothing_withoutVehicle() = runTest {
        assertThat(evaluate()).isEqualTo(0)
        assertThat(notifier.shown).isEmpty()
    }

    @Test
    fun inspection_notifiesEachStageOnce_andRestartsForNewDate() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(10_000)))
        val vehicleId = vehicles.observePrimaryVehicle().first()!!.id
        inspections.setUserDueDate(vehicleId, today.plusDays(20))

        assertThat(evaluate()).isEqualTo(1)
        assertThat(evaluate()).isEqualTo(0)
        assertThat(inspections.notified).isEqualTo(InspectionReminderStage.Days30)

        inspections.setUserDueDate(vehicleId, today.plusDays(5))
        evaluate()

        assertThat(inspections.notified).isEqualTo(InspectionReminderStage.Days7)
        assertThat(notifier.inspectionShown.map { it.daysLeft }).containsExactly(20L, 5L).inOrder()
    }

    @Test
    fun inspection_farAway_isNotNotified() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(10_000)))
        inspections.setUserDueDate(vehicles.observePrimaryVehicle().first()!!.id, today.plusDays(90))

        assertThat(evaluate()).isEqualTo(0)
        assertThat(notifier.inspectionShown).isEmpty()
    }
}
