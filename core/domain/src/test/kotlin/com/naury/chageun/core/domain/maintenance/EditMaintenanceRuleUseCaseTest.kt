package com.naury.chageun.core.domain.maintenance

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceThresholds
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EditMaintenanceRuleUseCaseTest {

    private val repository = FakeMaintenanceRepository()
    private val vehicles = FakeVehicleRepository()
    private val editRule = EditMaintenanceRuleUseCase(repository, vehicles)
    private val vehicleId = VehicleId("vehicle-1")
    private val customThresholds =
        MaintenanceThresholds(dueSoonKm = 300, dueSoonDays = 7, upcomingKm = 1_000, upcomingDays = 30)

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Electric, Kilometers(10_000)))
        repository.inputs.value = MaintenanceInputs(
            rules = listOf(
                MaintenanceRule(MaintenanceItem.EngineOil, 10_000, 12, thresholds = customThresholds),
                MaintenanceRule(MaintenanceItem.Tire, 50_000, 48, source = RuleSource.User),
            ),
            lastServices = emptyMap(),
            mileageHistory = emptyList(),
        )
    }

    private fun rule(item: MaintenanceItem) = repository.inputs.value.rules.single { it.item == item }

    @Test
    fun savesUserIntervals_keepingThresholds() = runTest {
        val error = editRule.update(vehicleId, MaintenanceItem.EngineOil, 7_000, null, isEnabled = true)

        assertThat(error).isNull()
        assertThat(rule(MaintenanceItem.EngineOil)).isEqualTo(
            MaintenanceRule(
                MaintenanceItem.EngineOil,
                7_000,
                null,
                thresholds = customThresholds,
                source = RuleSource.User,
            ),
        )
    }

    @Test
    fun rejectsMissingOrNonPositiveIntervals() = runTest {
        assertThat(
            editRule.update(vehicleId, MaintenanceItem.EngineOil, null, null, true),
        ).isEqualTo(RuleEditError.NoInterval)
        assertThat(
            editRule.update(vehicleId, MaintenanceItem.EngineOil, 0, 12, true),
        ).isEqualTo(RuleEditError.NonPositiveInterval)
        assertThat(rule(MaintenanceItem.EngineOil).source).isEqualTo(RuleSource.Generic)
    }

    @Test
    fun disablesItem() = runTest {
        editRule.update(vehicleId, MaintenanceItem.EngineOil, 10_000, 12, isEnabled = false)

        assertThat(rule(MaintenanceItem.EngineOil).isEnabled).isFalse()
    }

    @Test
    fun resetsToGenericForVehicleFuel() = runTest {
        editRule.resetToGeneric(vehicleId, MaintenanceItem.Tire)
        editRule.resetToGeneric(vehicleId, MaintenanceItem.EngineOil)

        assertThat(rule(MaintenanceItem.Tire)).isEqualTo(MaintenanceRule(MaintenanceItem.Tire, 50_000, 48))
        assertThat(rule(MaintenanceItem.EngineOil).isEnabled).isFalse()
    }
}
