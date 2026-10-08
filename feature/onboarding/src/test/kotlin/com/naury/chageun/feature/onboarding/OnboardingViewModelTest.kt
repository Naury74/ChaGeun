package com.naury.chageun.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.vehicle.RegistrationValidator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeVehiclePhotoRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    private val analytics = FakeAnalyticsTracker()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeVehicleRepository()
    private val photos = FakeVehiclePhotoRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 1)

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        OnboardingViewModel(handle, repository, photos, RegistrationValidator(clock), clock, analytics)

    private fun OnboardingViewModel.reachQuickMaintenance(mileage: String = "42180") {
        onAction(OnboardingAction.Start)
        onAction(OnboardingAction.SkipPlate)
        fillVehicleInfo()
        onAction(OnboardingAction.SubmitVehicleInfo)
        onAction(OnboardingAction.SubmitPhoto)
        onAction(OnboardingAction.MileageChanged(mileage))
        onAction(OnboardingAction.SubmitMileage)
    }

    private fun OnboardingViewModel.fillVehicleInfo() {
        onAction(OnboardingAction.MakerChanged("KG Mobility"))
        onAction(OnboardingAction.ModelChanged("Torres"))
        onAction(OnboardingAction.ModelYearChanged("2023"))
        onAction(OnboardingAction.FuelTypeSelected(FuelType.Gasoline))
    }

    @Test
    fun offersMileageEstimate_fromModelYear() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.ModelYearChanged("2023"))

        assertThat(vm.uiState.value.mileageEstimate).isEqualTo(45_000)
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
        vm.onAction(OnboardingAction.SubmitPhoto)
        vm.onAction(OnboardingAction.MileageChanged("42,180"))
        vm.onAction(OnboardingAction.SubmitMileage)
        vm.onAction(OnboardingAction.SubmitQuickMaintenance)
        vm.onAction(OnboardingAction.Finish)

        val registration = repository.registrations.single()
        assertThat(registration.plate?.normalized).isEqualTo("123가4567")
        assertThat(registration.currentMileage).isEqualTo(Kilometers(42_180))
        assertThat(registration.model).isEqualTo("Torres")
        assertThat(analytics.events).containsExactly(
            AnalyticsEvent.OnboardingStarted,
            AnalyticsEvent.ManualRegistrationUsed,
            AnalyticsEvent.OnboardingCompleted(withPlate = true, knownServiceCount = 0),
        ).inOrder()
    }

    @Test
    fun startsOver_afterRegistration() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.PlateChanged("123가4567"))
        vm.onAction(OnboardingAction.SubmitPlate)
        vm.fillVehicleInfo()
        vm.onAction(OnboardingAction.SubmitVehicleInfo)
        vm.onAction(OnboardingAction.SubmitPhoto)
        vm.onAction(OnboardingAction.MileageChanged("42180"))
        vm.onAction(OnboardingAction.SubmitMileage)
        vm.onAction(OnboardingAction.SubmitQuickMaintenance)
        vm.onAction(OnboardingAction.Finish)

        assertThat(repository.registrations).hasSize(1)
        assertThat(vm.uiState.value).isEqualTo(OnboardingUiState())
    }

    @Test
    fun showsRetryableError_whenSaveFails() {
        repository.failNextRegistration = true
        val vm = viewModel()
        vm.reachQuickMaintenance()
        vm.onAction(OnboardingAction.SubmitQuickMaintenance)
        vm.onAction(OnboardingAction.Finish)

        assertThat(vm.uiState.value.hasSaveFailed).isTrue()
        assertThat(vm.uiState.value.isSaving).isFalse()
        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.Notifications)
        assertThat(analytics.events).doesNotContain(AnalyticsEvent.ManualRegistrationUsed)
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
    fun registersOnlyKnownQuickServices() {
        val vm = viewModel()
        vm.reachQuickMaintenance()
        vm.onAction(OnboardingAction.QuickServiceModeSelected(MaintenanceItem.EngineOil, QuickServiceMode.Exact))
        vm.onAction(OnboardingAction.QuickServiceDateSelected(MaintenanceItem.EngineOil, LocalDate.of(2026, 3, 10)))
        vm.onAction(OnboardingAction.QuickServiceMileageChanged(MaintenanceItem.EngineOil, "40260"))
        vm.onAction(OnboardingAction.QuickServiceModeSelected(MaintenanceItem.Tire, QuickServiceMode.HalfYear))
        vm.onAction(OnboardingAction.SubmitQuickMaintenance)
        vm.onAction(OnboardingAction.Finish)

        val services = repository.registrations.single().knownServices
        assertThat(services.keys).containsExactly(MaintenanceItem.EngineOil, MaintenanceItem.Tire)
        assertThat(services.getValue(MaintenanceItem.EngineOil))
            .isEqualTo(ServiceRecord(LocalDate.of(2026, 3, 10), Kilometers(40_260)))
        // 2023년식 42,180 km면 하루 약 30.8 km. 6개월(183일) 전은 약 5,638 km 전이다.
        assertThat(services.getValue(MaintenanceItem.Tire))
            .isEqualTo(ServiceRecord(today.minusMonths(6), Kilometers(36_542), isMileageEstimated = true))
    }

    @Test
    fun approximateChoice_dropsExactValues() {
        val vm = viewModel()
        vm.reachQuickMaintenance()
        vm.onAction(OnboardingAction.QuickServiceModeSelected(MaintenanceItem.Battery, QuickServiceMode.Exact))
        vm.onAction(OnboardingAction.QuickServiceDateSelected(MaintenanceItem.Battery, LocalDate.of(2025, 1, 1)))
        vm.onAction(OnboardingAction.QuickServiceMileageChanged(MaintenanceItem.Battery, "30000"))
        vm.onAction(OnboardingAction.QuickServiceModeSelected(MaintenanceItem.Battery, QuickServiceMode.OneYear))
        vm.onAction(OnboardingAction.SubmitQuickMaintenance)
        vm.onAction(OnboardingAction.Finish)

        assertThat(repository.registrations.single().knownServices.getValue(MaintenanceItem.Battery))
            .isEqualTo(ServiceRecord(today.minusMonths(12), Kilometers(30_934), isMileageEstimated = true))
    }

    @Test
    fun rejectsExactMileage_aboveCurrentMileage_andMissingDate() {
        val vm = viewModel()
        vm.reachQuickMaintenance(mileage = "30000")
        vm.onAction(OnboardingAction.QuickServiceModeSelected(MaintenanceItem.EngineOil, QuickServiceMode.Exact))
        vm.onAction(OnboardingAction.QuickServiceDateSelected(MaintenanceItem.EngineOil, LocalDate.of(2026, 3, 10)))
        vm.onAction(OnboardingAction.QuickServiceMileageChanged(MaintenanceItem.EngineOil, "31000"))
        vm.onAction(OnboardingAction.QuickServiceModeSelected(MaintenanceItem.Tire, QuickServiceMode.Exact))
        vm.onAction(OnboardingAction.SubmitQuickMaintenance)

        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.QuickMaintenance)
        assertThat(vm.uiState.value.quickServiceErrors).containsExactly(
            MaintenanceItem.EngineOil,
            FieldError.ExceedsCurrentMileage,
            MaintenanceItem.Tire,
            FieldError.Required,
        )
    }

    @Test
    fun rejectsFutureServiceDate() {
        val vm = viewModel()
        vm.reachQuickMaintenance()
        vm.onAction(OnboardingAction.QuickServiceModeSelected(MaintenanceItem.Battery, QuickServiceMode.Exact))
        vm.onAction(OnboardingAction.QuickServiceDateSelected(MaintenanceItem.Battery, today.plusDays(1)))
        vm.onAction(OnboardingAction.SubmitQuickMaintenance)

        assertThat(vm.uiState.value.quickServiceErrors)
            .containsExactly(MaintenanceItem.Battery, FieldError.FutureDate)
    }

    @Test
    fun restoresQuickServiceDraft_afterProcessDeath() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            reachQuickMaintenance()
            onAction(OnboardingAction.QuickServiceModeSelected(MaintenanceItem.Tire, QuickServiceMode.Exact))
            onAction(OnboardingAction.QuickServiceDateSelected(MaintenanceItem.Tire, LocalDate.of(2024, 8, 5)))
        }

        val tire = viewModel(handle).uiState.value.quickServices.getValue(MaintenanceItem.Tire)

        assertThat(tire).isEqualTo(QuickServiceInput(QuickServiceMode.Exact, LocalDate.of(2024, 8, 5)))
    }

    @Test
    fun goesBackOneStep() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.SkipPlate)
        vm.onAction(OnboardingAction.Back)

        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.Plate)
    }

    @Test
    fun photoStep_comesAfterVehicleInfo_andCanBeSkipped() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Start)
        vm.onAction(OnboardingAction.SkipPlate)
        vm.fillVehicleInfo()
        vm.onAction(OnboardingAction.SubmitVehicleInfo)
        val onPhoto = vm.uiState.value.step
        vm.onAction(OnboardingAction.SubmitPhoto)

        assertThat(onPhoto).isEqualTo(OnboardingStep.Photo)
        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.Mileage)
        assertThat(vm.uiState.value.photoUri).isNull()
    }

    @Test
    fun pickedPhoto_survivesRecreation_andIsImportedAfterRegistering() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.onAction(OnboardingAction.Start)
        first.onAction(OnboardingAction.SkipPlate)
        first.fillVehicleInfo()
        first.onAction(OnboardingAction.SubmitVehicleInfo)
        first.onAction(OnboardingAction.PhotoPicked("file:///cache/photo_input/car.jpg"))

        val recreated = viewModel(handle)
        recreated.onAction(OnboardingAction.SubmitPhoto)
        recreated.onAction(OnboardingAction.MileageChanged("42180"))
        recreated.onAction(OnboardingAction.SubmitMileage)
        recreated.onAction(OnboardingAction.SubmitQuickMaintenance)
        recreated.onAction(OnboardingAction.Finish)

        assertThat(photos.replacedInBackground.map { it.second }).containsExactly("file:///cache/photo_input/car.jpg")
    }

    @Test
    fun removedPhoto_isNotImported() {
        val vm = viewModel()
        vm.reachQuickMaintenance()
        vm.onAction(OnboardingAction.PhotoPicked("file:///car.jpg"))
        vm.onAction(OnboardingAction.RemovePhoto)
        vm.onAction(OnboardingAction.SubmitQuickMaintenance)
        vm.onAction(OnboardingAction.Finish)

        assertThat(photos.replacedInBackground).isEmpty()
    }
}
