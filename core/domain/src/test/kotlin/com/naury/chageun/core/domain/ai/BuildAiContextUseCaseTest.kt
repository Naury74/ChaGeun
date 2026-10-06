package com.naury.chageun.core.domain.ai

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.PlateNumber
import com.naury.chageun.core.model.PlateParseResult
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BuildAiContextUseCaseTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val history = FakeHistoryRepository()
    private val inspections = FakeInspectionRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val buildContext = BuildAiContextUseCase(
        vehicles,
        ObserveMaintenanceOverviewUseCase(
            maintenance,
            inspections,
            RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
            VehicleHealthAggregator(),
            clock,
        ),
        history,
        inspections,
        clock,
    )

    @Before
    fun setUp() = runTest {
        val plate = (PlateNumber.parse("123가4567") as PlateParseResult.Valid).plate
        vehicles.register(
            VehicleRegistration("KG Mobility", "Torres", 2023, FuelType.Gasoline, Kilometers(49_700), plate = plate),
        )
        maintenance.inputs.value = MaintenanceInputs(
            rules = listOf(
                MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.Wiper, intervalKm = null, intervalMonths = 12),
                MaintenanceRule(MaintenanceItem.Tire, intervalKm = 50_000, intervalMonths = 48),
            ),
            lastServices = mapOf(
                MaintenanceItem.EngineOil to ServiceRecord(today.minusMonths(6), Kilometers(40_000)),
                MaintenanceItem.Wiper to ServiceRecord(today.minusMonths(1), null),
            ),
            mileageHistory = listOf(MileageReading(today, Kilometers(49_700))),
        )
        history.timeline.value = listOf(
            TimelineItem(
                RecordRef(TimelineEventType.Fuel, "f1"),
                today,
                "Station near home",
                null,
                Kilometers(49_700),
                70_000,
                RecordSource.User,
                Instant.EPOCH,
            ),
            TimelineItem(
                RecordRef(TimelineEventType.Repair, "r1"),
                today.minusDays(3),
                "Bumper",
                null,
                null,
                120_000,
                RecordSource.User,
                Instant.EPOCH,
            ),
        )
    }

    @Test
    fun sharesOnlyItemsThatNeedAttention_andListsMissingInfo() = runTest {
        val facts = buildContext(flowOf(AiContextOptions())).first()

        assertThat(facts.maintenance.map { it.item }).containsExactly(MaintenanceItem.EngineOil)
        assertThat(facts.missingInfo).containsExactly(MaintenanceItem.Tire)
        assertThat(facts.mileage?.mileage).isEqualTo(Kilometers(49_700))
    }

    @Test
    fun neverCarriesPlate_stationName_orCostsUnlessAllowed() = runTest {
        val facts = buildContext(flowOf(AiContextOptions())).first()

        assertThat(facts.toString()).doesNotContain("4567")
        assertThat(facts.recentRecords.first { it.type == TimelineEventType.Fuel }.title).isNull()
        assertThat(facts.recentRecords.map { it.costWon }).containsExactly(null, null)

        val withCosts = buildContext(flowOf(AiContextOptions(includeCosts = true))).first()
        assertThat(withCosts.recentRecords.map { it.costWon }).containsExactly(70_000L, 120_000L)
    }

    @Test
    fun costTotals_coverThisYearOnly_andOnlyWhenCostsAreShared() = runTest {
        history.timeline.value += TimelineItem(
            RecordRef(TimelineEventType.Maintenance, "last-year"),
            LocalDate.of(2025, 12, 20),
            null,
            MaintenanceItem.EngineOil,
            Kilometers(45_000),
            90_000,
            RecordSource.User,
            Instant.EPOCH,
        )

        assertThat(buildContext(flowOf(AiContextOptions())).first().costTotals).isNull()
        val totals = buildContext(flowOf(AiContextOptions(includeCosts = true))).first().costTotals
        // 작년 엔진오일은 빼고, 수리는 정비 쪽에, 주유는 따로 센다.
        assertThat(totals).isEqualTo(CostTotals(year = 2026, maintenanceWon = 120_000, fuelWon = 70_000))
        val recordsOff = AiContextOptions(includeCosts = true, includeRecords = false)
        assertThat(buildContext(flowOf(recordsOff)).first().costTotals).isNull()
    }

    @Test
    fun sharesInspectionSchedule_whenKnown() = runTest {
        assertThat(buildContext(flowOf(AiContextOptions())).first().inspection).isNull()

        inspections.schedule.value = InspectionSchedule(today.plusDays(20), InspectionSource.User)

        assertThat(buildContext(flowOf(AiContextOptions())).first().inspection?.daysLeft).isEqualTo(20)
    }

    @Test
    fun focusItem_limitsMaintenanceAndRecords() = runTest {
        val facts = buildContext(flowOf(AiContextOptions(focusItem = MaintenanceItem.EngineOil))).first()

        assertThat(facts.maintenance.map { it.item }).containsExactly(MaintenanceItem.EngineOil)
        assertThat(facts.missingInfo).isEmpty()
        assertThat(facts.recentRecords).isEmpty()
    }

    @Test
    fun omitsSectionsTurnedOff() = runTest {
        val facts = buildContext(flowOf(AiContextOptions(includeMaintenance = false, includeRecords = false))).first()

        assertThat(facts.maintenance).isEmpty()
        assertThat(facts.recentRecords).isEmpty()
    }

    @Test
    fun focusRecord_fuel_sharesAmountsButNotStationOrMemo() = runTest {
        val ref = RecordRef(TimelineEventType.Fuel, "f1")
        history.details.value = mapOf(
            ref to RecordDetail.Fuel(
                ref,
                FuelEntry(
                    today,
                    Kilometers(49_700),
                    FuelAmounts(70_000, 41_176, 1_700, FuelField.Volume),
                    isFullTank = true,
                    stationName = "Station near home",
                    memo = "Picked up kids",
                ),
            ),
        )

        val facts = buildContext(flowOf(AiContextOptions(focusRecord = ref))).first()
        val withCosts = buildContext(flowOf(AiContextOptions(focusRecord = ref, includeCosts = true))).first()

        val record = facts.focusRecord!!
        assertThat(record.fuelVolumeMl).isEqualTo(41_176)
        assertThat(record.isFullTank).isTrue()
        assertThat(record.costWon).isNull()
        assertThat(record.fuelUnitPriceWon).isNull()
        assertThat(withCosts.focusRecord?.costWon).isEqualTo(70_000)
        assertThat(facts.toString()).doesNotContain("Station near home")
        assertThat(facts.toString()).doesNotContain("Picked up kids")
        // 질문 대상 기록은 최근 기록 목록에 다시 넣지 않는다.
        assertThat(facts.recentRecords.map { it.type }).doesNotContain(TimelineEventType.Fuel)
    }

    @Test
    fun focusRecord_maintenance_narrowsToThatItem() = runTest {
        val ref = RecordRef(TimelineEventType.Maintenance, "m1")
        history.details.value = mapOf(
            ref to RecordDetail.Maintenance(
                ref,
                MaintenanceItem.Wiper,
                ServiceHistoryEntry("m1", today.minusMonths(1), null, 30_000, "Blue Hands"),
                memo = "Front only",
            ),
        )

        val facts = buildContext(flowOf(AiContextOptions(focusRecord = ref))).first()

        assertThat(facts.focusRecord?.maintenanceItem).isEqualTo(MaintenanceItem.Wiper)
        assertThat(facts.maintenance.map { it.item }).containsExactly(MaintenanceItem.Wiper)
        assertThat(facts.missingInfo).isEmpty()
        assertThat(facts.toString()).doesNotContain("Blue Hands")
        assertThat(facts.toString()).doesNotContain("Front only")
    }
}
