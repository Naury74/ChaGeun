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
        again.onRemovePlate()
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
