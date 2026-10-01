package com.naury.chageun.core.domain.ai

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.PlateNumber
import com.naury.chageun.core.model.PlateParseResult
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeHistoryRepository
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
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val buildContext = BuildAiContextUseCase(
        vehicles,
        ObserveMaintenanceOverviewUseCase(
            maintenance,
            RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
            VehicleHealthAggregator(),
            clock,
        ),
        history,
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
}
