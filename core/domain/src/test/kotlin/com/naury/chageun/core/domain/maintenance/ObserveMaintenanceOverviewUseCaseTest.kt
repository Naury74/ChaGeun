package com.naury.chageun.core.domain.maintenance

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveMaintenanceOverviewUseCaseTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val inputs = MutableStateFlow(MaintenanceInputs(emptyList(), emptyMap(), emptyList()))
    private val repository = object : MaintenanceRepository {
        override fun observeInputs(vehicleId: VehicleId): Flow<MaintenanceInputs> = inputs
    }
    private val useCase = ObserveMaintenanceOverviewUseCase(
        repository = repository,
        engine = RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
        healthAggregator = VehicleHealthAggregator(),
        clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC),
    )

    private fun rule(item: MaintenanceItem, km: Long? = 10_000, months: Long? = 12, enabled: Boolean = true) =
        MaintenanceRule(item, intervalKm = km, intervalMonths = months, isEnabled = enabled)

    private suspend fun overview() = useCase(VehicleId("v1")).first()

    @Test
    fun sortsByUrgency_thenSafetyItemsFirst() = runTest {
        inputs.value = MaintenanceInputs(
            rules = listOf(
                rule(MaintenanceItem.Wiper, km = null),
                rule(MaintenanceItem.EngineOil),
                rule(MaintenanceItem.Tire, km = 50_000, months = 48),
                rule(MaintenanceItem.CabinFilter),
            ),
            lastServices = mapOf(
                MaintenanceItem.EngineOil to ServiceRecord(today.minusMonths(2), Kilometers(31_000)),
                MaintenanceItem.CabinFilter to ServiceRecord(today.minusMonths(2), Kilometers(44_000)),
                MaintenanceItem.Tire to ServiceRecord(today.minusYears(5), Kilometers(10_000)),
            ),
            mileageHistory = listOf(MileageReading(today, Kilometers(45_000))),
        )

        val items = overview().statuses.map { it.item to it.state }

        assertThat(items).containsExactly(
            MaintenanceItem.Tire to MaintenanceState.Overdue,
            MaintenanceItem.EngineOil to MaintenanceState.Overdue,
            MaintenanceItem.Wiper to MaintenanceState.Unknown,
            MaintenanceItem.CabinFilter to MaintenanceState.Good,
        ).inOrder()
    }

    @Test
    fun aggregatesVehicleHealth_andCurrentMileage() = runTest {
        inputs.value = MaintenanceInputs(
            rules = listOf(rule(MaintenanceItem.EngineOil)),
            lastServices = mapOf(MaintenanceItem.EngineOil to ServiceRecord(today.minusMonths(1), Kilometers(44_000))),
            mileageHistory = listOf(
                MileageReading(today.minusDays(30), Kilometers(44_000)),
                MileageReading(today.minusDays(1), Kilometers(45_000)),
            ),
        )

        val overview = overview()

        assertThat(overview.health.level).isEqualTo(VehicleHealthLevel.Good)
        assertThat(overview.currentMileage).isEqualTo(MileageReading(today.minusDays(1), Kilometers(45_000)))
    }

    @Test
    fun skipsDisabledRules() = runTest {
        inputs.value = MaintenanceInputs(
            rules = listOf(rule(MaintenanceItem.EngineOil, enabled = false), rule(MaintenanceItem.Battery, km = null)),
            lastServices = emptyMap(),
            mileageHistory = emptyList(),
        )

        assertThat(overview().statuses.map { it.item }).containsExactly(MaintenanceItem.Battery)
    }

    @Test
    fun reportsInsufficientData_forNewVehicleWithoutHistory() = runTest {
        inputs.value = MaintenanceInputs(
            rules = listOf(rule(MaintenanceItem.EngineOil), rule(MaintenanceItem.Tire)),
            lastServices = emptyMap(),
            mileageHistory = listOf(MileageReading(today, Kilometers(45_000))),
        )

        assertThat(overview().health.level).isEqualTo(VehicleHealthLevel.InsufficientData)
    }
}
