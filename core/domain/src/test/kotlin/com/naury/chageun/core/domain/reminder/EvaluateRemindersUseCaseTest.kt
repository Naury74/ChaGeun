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
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeMileageReminderLog
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

        override fun cancelInspection() = Unit

        val cancelled = mutableListOf<MaintenanceItem>()

        override fun cancel(item: MaintenanceItem) {
            cancelled += item
        }

        var mileagePrompts = 0

        override fun notifyMileagePrompt() {
            mileagePrompts++
        }
    }
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val mileageLog = FakeMileageReminderLog()
    private val evaluate = EvaluateRemindersUseCase(
        vehicles,
        ObserveMaintenanceOverviewUseCase(
            maintenance,
            inspections,
            RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
            VehicleHealthAggregator(),
            clock,
        ),
        notifier,
        settings,
        NotifyDueItemsUseCase(reminders, inspections, notifier),
        PromptMileageUpdateUseCase(notifier, mileageLog, clock),
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
        // 교체까지 300km 남아 500km 단계에 해당한다.
        assertThat(notifier.shown.single().remainingKm).isEqualTo(300)
        assertThat(reminders.stages).containsExactly(MaintenanceItem.EngineOil, MaintenanceReminderStage.Near)
    }

    @Test
    fun clearsNotification_whenItemIsBackToGood() = runTest {
        givenOilDueSoon()
        evaluate()

        maintenance.inputs.value = maintenance.inputs.value.copy(
            lastServices = mapOf(MaintenanceItem.EngineOil to ServiceRecord(today, Kilometers(49_700))),
        )
        evaluate()

        assertThat(notifier.cancelled).containsExactly(MaintenanceItem.EngineOil)
        assertThat(reminders.stages).isEmpty()
    }

    @Test
    fun recordsNothing_whenNotificationsAreBlocked() = runTest {
        givenOilDueSoon()
        notifier.enabled = false

        assertThat(evaluate()).isEqualTo(0)
        assertThat(reminders.stages).isEmpty()
    }

    @Test
    fun staysSilent_whenUserTurnedRemindersOff() = runTest {
        givenOilDueSoon()
        settings.setMaintenanceReminderEnabled(false)

        assertThat(evaluate()).isEqualTo(0)
        assertThat(notifier.shown).isEmpty()
    }

    @Test
    fun inspectionReminders_canBeTurnedOffSeparately() = runTest {
        givenOilDueSoon()
        inspections.setUserDueDate(vehicles.observePrimaryVehicle().first()!!.id, today.plusDays(5))
        settings.setInspectionReminderEnabled(false)

        assertThat(evaluate()).isEqualTo(1)
        assertThat(notifier.shown.map { it.item }).containsExactly(MaintenanceItem.EngineOil)
        assertThat(notifier.inspectionShown).isEmpty()
    }

    @Test
    fun maintenanceOff_stillSendsInspectionReminders() = runTest {
        givenOilDueSoon()
        inspections.setUserDueDate(vehicles.observePrimaryVehicle().first()!!.id, today.plusDays(5))
        settings.setMaintenanceReminderEnabled(false)

        assertThat(evaluate()).isEqualTo(1)
        assertThat(notifier.shown).isEmpty()
        assertThat(notifier.inspectionShown).hasSize(1)
        // 꺼 둔 동안에는 단계를 기록하지 않아, 다시 켜면 그때 도달한 단계부터 알린다.
        assertThat(reminders.stages).isEmpty()
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

    @Test
    fun mileagePrompt_isOffByDefault() = runTest {
        givenMileageReadOn(today.minusDays(45))

        evaluate()

        assertThat(notifier.mileagePrompts).isEqualTo(0)
    }

    @Test
    fun mileagePrompt_onceAMonth_whenReadingIsStale() = runTest {
        settings.setMileageReminderEnabled(true)
        givenMileageReadOn(today.minusDays(45))

        evaluate()
        evaluate()

        assertThat(notifier.mileagePrompts).isEqualTo(1)
        assertThat(mileageLog.lastNotified).isEqualTo(today)
    }

    @Test
    fun mileagePrompt_skipsRecentReading_andRecentPrompt() = runTest {
        settings.setMileageReminderEnabled(true)
        givenMileageReadOn(today.minusDays(10))
        evaluate()

        givenMileageReadOn(today.minusDays(45))
        mileageLog.lastNotified = today.minusDays(20)
        evaluate()

        assertThat(notifier.mileagePrompts).isEqualTo(0)
    }

    @Test
    fun mileagePrompt_worksWithMaintenanceRemindersOff() = runTest {
        settings.setMaintenanceReminderEnabled(false)
        settings.setMileageReminderEnabled(true)
        givenMileageReadOn(today.minusDays(45))

        assertThat(evaluate()).isEqualTo(1)
    }

    private suspend fun givenMileageReadOn(date: LocalDate) {
        if (vehicles.observePrimaryVehicle().first() == null) {
            vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(10_000)))
        }
        maintenance.inputs.value = MaintenanceInputs(
            rules = emptyList(),
            lastServices = emptyMap(),
            mileageHistory = listOf(MileageReading(date, Kilometers(10_000))),
        )
    }
}
