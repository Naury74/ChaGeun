package com.naury.chageun.feature.vehicle.edit

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.vehicle.RegistrationValidator
import com.naury.chageun.core.domain.vehicle.UpdateVehicleUseCase
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.PlateNumber
import com.naury.chageun.core.model.PlateParseResult
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VehicleEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)

    private fun viewModel() = VehicleEditViewModel(
        vehicles,
        UpdateVehicleUseCase(vehicles, FakeMaintenanceRepository(), RegistrationValidator(clock)),
    )

    @Before
    fun setUp() = runTest {
        val plate = (PlateNumber.parse("12가3456") as PlateParseResult.Valid).plate
        vehicles.register(
            VehicleRegistration("Hyundai", "Avante", 2022, FuelType.Gasoline, Kilometers(30_000), plate, "Modern"),
        )
    }

    @Test
    fun startsFromCurrentVehicle() {
        val state = viewModel().uiState.value

        assertThat(state.isLoaded).isTrue()
        assertThat(listOf(state.maker, state.model, state.modelYear, state.trim))
            .containsExactly("Hyundai", "Avante", "2022", "Modern").inOrder()
        assertThat(state.currentPlateMasked).isEqualTo("12가 **56")
        assertThat(state.newPlate).isEmpty()
    }

    @Test
    fun savesChanges_andKeepsPlateWhenLeftEmpty() = runTest {
        val viewModel = viewModel()
        viewModel.onModelChanged("Sonata")
        viewModel.onFuelTypeSelected(FuelType.Hybrid)

        viewModel.save()

        assertThat(viewModel.uiState.value.isSaved).isTrue()
        val saved = vehicles.observePrimaryVehicle().first()!!
        assertThat(saved.model).isEqualTo("Sonata")
        assertThat(saved.fuelType).isEqualTo(FuelType.Hybrid)
        assertThat(saved.plateMasked).isEqualTo("12가 **56")
    }

    @Test
    fun savesFirstRegistrationDate_andCanClearIt() = runTest {
        val viewModel = viewModel()
        viewModel.onFirstRegistrationDateChanged(LocalDate.of(2022, 3, 15))
        viewModel.save()

        assertThat(
            vehicles.observePrimaryVehicle().first()!!.firstRegistrationDate,
        ).isEqualTo(LocalDate.of(2022, 3, 15))

        val again = viewModel()
        assertThat(again.uiState.value.firstRegistrationDate).isEqualTo(LocalDate.of(2022, 3, 15))
        again.onFirstRegistrationDateChanged(null)
        again.save()

        assertThat(vehicles.observePrimaryVehicle().first()!!.firstRegistrationDate).isNull()
    }

    @Test
    fun savesDisplacement_andClearsItWhenEmptied() = runTest {
        val viewModel = viewModel()
        viewModel.onDisplacementChanged("1,598cc")
        assertThat(viewModel.uiState.value.displacementCc).isEqualTo("1598")
        viewModel.save()

        assertThat(vehicles.observePrimaryVehicle().first()!!.displacementCc).isEqualTo(1_598)

        val again = viewModel()
        assertThat(again.uiState.value.displacementCc).isEqualTo("1598")
        again.onDisplacementChanged("")
        again.save()

        assertThat(again.uiState.value.isSaved).isTrue()
        assertThat(vehicles.observePrimaryVehicle().first()!!.displacementCc).isNull()
    }

    @Test
    fun rejectsDisplacementOutOfRange_andKeepsAtMostFiveDigits() = runTest {
        val viewModel = viewModel()
        viewModel.onDisplacementChanged("1234567")
        assertThat(viewModel.uiState.value.displacementCc).isEqualTo("12345")

        viewModel.save()
        assertThat(viewModel.uiState.value.errors[VehicleEditField.Displacement])
            .isEqualTo(VehicleEditError.InvalidDisplacement)
        assertThat(viewModel.uiState.value.isSaved).isFalse()

        viewModel.onDisplacementChanged("49")
        assertThat(viewModel.uiState.value.errors).doesNotContainKey(VehicleEditField.Displacement)
        viewModel.save()
        assertThat(viewModel.uiState.value.errors).containsKey(VehicleEditField.Displacement)

        viewModel.onDisplacementChanged("50")
        viewModel.save()
        assertThat(viewModel.uiState.value.isSaved).isTrue()
        assertThat(vehicles.observePrimaryVehicle().first()!!.displacementCc).isEqualTo(50)
    }

    @Test
    fun hidesDisplacementForElectricAndHydrogen_andSavesItEmpty() = runTest {
        val viewModel = viewModel()
        viewModel.onDisplacementChanged("1598")
        assertThat(viewModel.uiState.value.hasEngine).isTrue()

        viewModel.onFuelTypeSelected(FuelType.Hydrogen)
        assertThat(viewModel.uiState.value.hasEngine).isFalse()
        viewModel.onFuelTypeSelected(FuelType.Electric)
        assertThat(viewModel.uiState.value.hasEngine).isFalse()
        viewModel.save()

        val saved = vehicles.observePrimaryVehicle().first()!!
        assertThat(saved.fuelType).isEqualTo(FuelType.Electric)
        assertThat(saved.displacementCc).isNull()
    }

    @Test
    fun ignoresInvalidDisplacement_whenFuelHasNoEngine() = runTest {
        val viewModel = viewModel()
        viewModel.onDisplacementChanged("12")
        viewModel.onFuelTypeSelected(FuelType.Electric)

        viewModel.save()

        assertThat(viewModel.uiState.value.isSaved).isTrue()
        assertThat(vehicles.observePrimaryVehicle().first()!!.displacementCc).isNull()
    }

    @Test
    fun showsFuelNotice_onlyWhenFuelChanges() {
        val viewModel = viewModel()
        assertThat(viewModel.uiState.value.isFuelChanged).isFalse()

        viewModel.onFuelTypeSelected(FuelType.Electric)

        assertThat(viewModel.uiState.value.isFuelChanged).isTrue()
    }

    @Test
    fun blocksSave_forMissingModelAndBadPlate() = runTest {
        val viewModel = viewModel()
        viewModel.onModelChanged("")
        viewModel.onPlateChanged("1234")

        viewModel.save()

        val state = viewModel.uiState.value
        assertThat(state.isSaved).isFalse()
        assertThat(state.errors).containsExactly(
            VehicleEditField.Model,
            VehicleEditError.Required,
            VehicleEditField.Plate,
            VehicleEditError.InvalidPlate,
        )
        assertThat(vehicles.observePrimaryVehicle().first()!!.model).isEqualTo("Avante")
    }

    @Test
    fun replacesAndRemovesPlate() = runTest {
        val viewModel = viewModel()
        viewModel.onPlateChanged("34나 5678")
        viewModel.save()
        assertThat(vehicles.observePrimaryVehicle().first()!!.plateMasked).isEqualTo("34나 **78")

        val again = viewModel()
        again.onPlateRemovalChanged(remove = true)
        again.save()
        assertThat(vehicles.observePrimaryVehicle().first()!!.plateMasked).isNull()
    }

    @Test
    fun rejectsYearOutOfRange() = runTest {
        val viewModel = viewModel()
        viewModel.onModelYearChanged("1950")

        viewModel.save()

        assertThat(viewModel.uiState.value.errors)
            .containsExactly(VehicleEditField.ModelYear, VehicleEditError.InvalidYear)
    }
}
