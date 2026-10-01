package com.naury.chageun.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.vehicle.RegistrationValidator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeVehicleRepository()
    private val validator = RegistrationValidator(Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC))

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        OnboardingViewModel(handle, repository, validator)

    private fun OnboardingViewModel.fillVehicleInfo() {
        onAction(OnboardingAction.MakerChanged("KG Mobility"))
        onAction(OnboardingAction.ModelChanged("Torres"))
        onAction(OnboardingAction.ModelYearChanged("2023"))
        onAction(OnboardingAction.FuelTypeSelected(FuelType.Gasoline))
    }

    @Test
    fun keepsPlateStep_withoutError_whileHangulIsComposing() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.PlateChanged("123ㄱ"))

        assertThat(vm.uiState.value.errors).isEmpty()
        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.Plate)
    }

    @Test
    fun rejectsInvalidPlate_onSubmit() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.PlateChanged("1가4567"))
        vm.onAction(OnboardingAction.SubmitPlate)

        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.Plate)
        assertThat(vm.uiState.value.errors).containsExactly(OnboardingField.Plate, FieldError.InvalidPlate)
    }

    @Test
    fun clearsFieldError_whenUserEdits() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.PlateChanged("bad"))
        vm.onAction(OnboardingAction.SubmitPlate)
        vm.onAction(OnboardingAction.PlateChanged("123가4567"))

        assertThat(vm.uiState.value.errors).isEmpty()
    }

    @Test
    fun reportsAllMissingVehicleFields() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.SkipPlate)
        vm.onAction(OnboardingAction.SubmitVehicleInfo)

        assertThat(vm.uiState.value.errors.keys).containsExactly(
            OnboardingField.Maker,
            OnboardingField.Model,
            OnboardingField.ModelYear,
            OnboardingField.FuelType,
        )
    }

    @Test
    fun rejectsFutureModelYear() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.SkipPlate)
        vm.fillVehicleInfo()
        vm.onAction(OnboardingAction.ModelYearChanged("2030"))
        vm.onAction(OnboardingAction.SubmitVehicleInfo)

        assertThat(vm.uiState.value.errors).containsExactly(OnboardingField.ModelYear, FieldError.InvalidYear)
    }

    @Test
    fun registersVehicle_withNormalizedPlateAndMileage() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.PlateChanged("123 가 4567"))
        vm.onAction(OnboardingAction.SubmitPlate)
        vm.fillVehicleInfo()
        vm.onAction(OnboardingAction.SubmitVehicleInfo)
        vm.onAction(OnboardingAction.MileageChanged("42,180"))
        vm.onAction(OnboardingAction.Finish)

        val registration = repository.registrations.single()
        assertThat(registration.plate?.normalized).isEqualTo("123가4567")
        assertThat(registration.currentMileage).isEqualTo(Kilometers(42_180))
        assertThat(registration.model).isEqualTo("Torres")
    }

    @Test
    fun showsRetryableError_whenSaveFails() {
        repository.failNextRegistration = true
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.SkipPlate)
        vm.fillVehicleInfo()
        vm.onAction(OnboardingAction.SubmitVehicleInfo)
        vm.onAction(OnboardingAction.MileageChanged("1000"))
        vm.onAction(OnboardingAction.Finish)

        assertThat(vm.uiState.value.hasSaveFailed).isTrue()
        assertThat(vm.uiState.value.isSaving).isFalse()
        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.Mileage)
    }

    @Test
    fun restoresDraft_afterProcessDeath() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            onAction(OnboardingAction.Start)
            onAction(OnboardingAction.SkipPlate)
            fillVehicleInfo()
        }

        val restored = viewModel(handle).uiState.value

        assertThat(restored.step).isEqualTo(OnboardingStep.VehicleInfo)
        assertThat(restored.model).isEqualTo("Torres")
        assertThat(restored.fuelType).isEqualTo(FuelType.Gasoline)
    }

    @Test
    fun goesBackOneStep() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.SkipPlate)
        vm.onAction(OnboardingAction.Back)

        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.Plate)
    }
}
