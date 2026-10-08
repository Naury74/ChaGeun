package com.naury.chageun.core.domain.vehicle

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.DefaultMaintenanceRules
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.PlateChange
import com.naury.chageun.core.model.PlateNumber
import com.naury.chageun.core.model.PlateParseResult
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleProfileUpdate
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateVehicleUseCaseTest {

    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val update = UpdateVehicleUseCase(
        vehicles,
        maintenance,
        RegistrationValidator(Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)),
    )

    private suspend fun registered(fuelType: FuelType): Vehicle {
        val plate = (PlateNumber.parse("12가3456") as PlateParseResult.Valid).plate
        vehicles.register(VehicleRegistration("Hyundai", "Avante", 2022, fuelType, Kilometers(30_000), plate))
        maintenance.inputs.value = MaintenanceInputs(DefaultMaintenanceRules.forFuel(fuelType), emptyMap(), emptyList())
        return vehicles.observePrimaryVehicle().first()!!
    }

    private fun profile(
        fuelType: FuelType = FuelType.Gasoline,
        modelYear: Int = 2022,
        plate: PlateChange = PlateChange.Keep,
    ) = VehicleProfileUpdate(
        "Kia",
        "K5",
        modelYear,
        fuelType,
        trim = "Signature",
        plate = plate,
        firstRegistrationDate = null,
        displacementCc = null,
    )

    @Test
    fun savesProfile_andKeepsPlate() = runTest {
        val vehicle = registered(FuelType.Gasoline)

        assertThat(update(vehicle, profile())).isEmpty()

        val saved = vehicles.observePrimaryVehicle().first()!!
        assertThat(listOf(saved.maker, saved.model, saved.trim)).containsExactly("Kia", "K5", "Signature").inOrder()
        assertThat(saved.plateMasked).isEqualTo(vehicle.plateMasked)
    }

    @Test
    fun removesPlate() = runTest {
        val vehicle = registered(FuelType.Gasoline)

        update(vehicle, profile(plate = PlateChange.Remove))

        assertThat(vehicles.observePrimaryVehicle().first()!!.plateMasked).isNull()
    }

    @Test
    fun rejectsOutOfRangeYear_withoutSaving() = runTest {
        val vehicle = registered(FuelType.Gasoline)

        assertThat(update(vehicle, profile(modelYear = 1900))).containsExactly(RegistrationError.ModelYearOutOfRange)
        assertThat(vehicles.observePrimaryVehicle().first()!!.maker).isEqualTo("Hyundai")
    }

    @Test
    fun switchingToElectric_turnsOffEngineItems_andKeepsOthers() = runTest {
        val vehicle = registered(FuelType.Gasoline)

        update(vehicle, profile(fuelType = FuelType.Electric))

        val rules = maintenance.inputs.value.rules.associateBy { it.item }
        assertThat(rules.getValue(MaintenanceItem.EngineOil).isEnabled).isFalse()
        assertThat(rules.getValue(MaintenanceItem.SparkPlug).isEnabled).isFalse()
        assertThat(rules.getValue(MaintenanceItem.Tire).isEnabled).isTrue()
    }

    @Test
    fun switchingFromElectric_addsMissingEngineItems() = runTest {
        val vehicle = registered(FuelType.Electric)

        update(vehicle, profile(fuelType = FuelType.Diesel))

        val rules = maintenance.inputs.value.rules.associateBy { it.item }
        assertThat(rules.getValue(MaintenanceItem.EngineOil).isEnabled).isTrue()
        assertThat(rules.getValue(MaintenanceItem.EngineOil).source).isEqualTo(RuleSource.Generic)
        // 디젤에는 점화 플러그가 없으므로 계속 빠져 있어야 한다.
        assertThat(rules[MaintenanceItem.SparkPlug]).isNull()
    }

    @Test
    fun keepsUserInterval_whenItemIsReEnabled() = runTest {
        val vehicle = registered(FuelType.Gasoline)
        val custom = maintenance.findRule(vehicle.id, MaintenanceItem.EngineOil)!!.copy(
            intervalKm = 7_000,
            isEnabled = false,
            source = RuleSource.User,
        )
        maintenance.saveRule(vehicle.id, custom)
        val electric = vehicle.copy(fuelType = FuelType.Electric)

        update(electric, profile(fuelType = FuelType.Hybrid))

        val oil = maintenance.findRule(vehicle.id, MaintenanceItem.EngineOil)!!
        assertThat(oil.isEnabled).isTrue()
        assertThat(oil.intervalKm).isEqualTo(7_000)
    }
}
