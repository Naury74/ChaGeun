package com.naury.chageun.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.vehicle.RegistrationError
import com.naury.chageun.core.domain.vehicle.RegistrationValidator
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.PlateNumber
import com.naury.chageun.core.model.PlateParseResult
import com.naury.chageun.core.model.VehicleRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val photoRepository: VehiclePhotoRepository,
    private val registrationValidator: RegistrationValidator,
    private val clock: Clock,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val draftStore = OnboardingDraftStore(savedStateHandle)
    private val _uiState = MutableStateFlow(draftStore.restore())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.Start -> {
                analytics.track(AnalyticsEvent.OnboardingStarted)
                moveTo(OnboardingStep.Plate)
            }
            OnboardingAction.Back -> goBack()
            is OnboardingAction.PlateChanged -> edit(OnboardingField.Plate) { copy(plate = action.value) }
            OnboardingAction.SubmitPlate -> submitPlate()
            OnboardingAction.SkipPlate -> {
                edit(OnboardingField.Plate) { copy(plate = "") }
                moveTo(OnboardingStep.VehicleInfo)
            }
            is OnboardingAction.MakerChanged -> edit(OnboardingField.Maker) { copy(maker = action.value) }
            is OnboardingAction.ModelChanged -> edit(OnboardingField.Model) { copy(model = action.value) }
            is OnboardingAction.ModelYearChanged ->
                edit(OnboardingField.ModelYear) {
                    val year = action.value.filter(Char::isDigit).take(YEAR_DIGITS)
                    copy(
                        modelYear = year,
                        mileageEstimate = MileageEstimate.fromModelYear(year.toIntOrNull(), LocalDate.now(clock)),
                    )
                }
            is OnboardingAction.FuelTypeSelected -> edit(OnboardingField.FuelType) { copy(fuelType = action.value) }
            OnboardingAction.SubmitVehicleInfo -> submitVehicleInfo()
            is OnboardingAction.PhotoPicked -> edit(null) { copy(photoUri = action.uri) }
            OnboardingAction.RemovePhoto -> edit(null) { copy(photoUri = null) }
            is OnboardingAction.PhotoBackgroundRemovalChanged ->
                edit(null) { copy(removePhotoBackground = action.enabled) }
            OnboardingAction.SubmitPhoto -> moveTo(OnboardingStep.Mileage)
            is OnboardingAction.MileageChanged ->
                edit(OnboardingField.Mileage) {
                    copy(mileage = action.value.filter(Char::isDigit).take(MILEAGE_DIGITS))
                }
            OnboardingAction.SubmitMileage -> submitMileage()
            is OnboardingAction.QuickServiceModeSelected -> editQuickService(action.item) {
                QuickServiceForm.select(this, action.mode, LocalDate.now(clock))
            }
            is OnboardingAction.QuickServiceDateSelected -> editQuickService(action.item) { copy(date = action.date) }
            is OnboardingAction.QuickServiceMileageChanged ->
                editQuickService(action.item) {
                    copy(mileage = action.value.filter(Char::isDigit).take(MILEAGE_DIGITS))
                }
            OnboardingAction.SubmitQuickMaintenance -> submitQuickMaintenance()
            OnboardingAction.Finish -> finish()
        }
    }

    private fun submitPlate() {
        when (val result = PlateNumber.parse(_uiState.value.plate)) {
            is PlateParseResult.Valid -> {
                edit(OnboardingField.Plate) { copy(plate = result.plate.normalized) }
                moveTo(OnboardingStep.VehicleInfo)
            }
            PlateParseResult.Empty -> moveTo(OnboardingStep.VehicleInfo)
            PlateParseResult.Composing, PlateParseResult.InvalidFormat ->
                setErrors(mapOf(OnboardingField.Plate to FieldError.InvalidPlate))
            PlateParseResult.UnsupportedUsage -> setErrors(mapOf(OnboardingField.Plate to FieldError.UnsupportedPlate))
        }
    }

    private fun submitVehicleInfo() {
        val state = _uiState.value
        val errors = buildMap {
            if (state.maker.isBlank()) put(OnboardingField.Maker, FieldError.Required)
            if (state.model.isBlank()) put(OnboardingField.Model, FieldError.Required)
            if (state.fuelType == null) put(OnboardingField.FuelType, FieldError.Required)
            val year = state.modelYear.toIntOrNull()
            when {
                year == null -> put(OnboardingField.ModelYear, FieldError.Required)
                RegistrationError.ModelYearOutOfRange in registrationValidator.validate(draft(state, year)) ->
                    put(OnboardingField.ModelYear, FieldError.InvalidYear)
            }
        }
        if (errors.isEmpty()) moveTo(OnboardingStep.Photo) else setErrors(errors)
    }

    private fun submitMileage() {
        val mileage = _uiState.value.mileage.toLongOrNull()
        when {
            mileage == null -> setErrors(mapOf(OnboardingField.Mileage to FieldError.Required))
            mileage > MAX_MILEAGE_KM -> setErrors(mapOf(OnboardingField.Mileage to FieldError.InvalidMileage))
            else -> moveTo(OnboardingStep.QuickMaintenance)
        }
    }

    private fun submitQuickMaintenance() {
        val state = _uiState.value
        val errors = QuickServiceForm.validate(state.quickServices, state.mileage.toLong(), LocalDate.now(clock))
        if (errors.isEmpty()) {
            moveTo(OnboardingStep.Notifications)
        } else {
            _uiState.update { it.copy(quickServiceErrors = errors) }
        }
    }

    private fun finish() {
        val state = _uiState.value
        val registration = draft(state, state.modelYear.toInt()).copy(
            currentMileage = Kilometers(state.mileage.toLong()),
            knownServices = QuickServiceForm.toServiceRecords(
                state.quickServices,
                currentMileage = state.mileage.toLong(),
                modelYear = state.modelYear.toIntOrNull(),
                today = LocalDate.now(clock),
            ),
        )
        _uiState.update { it.copy(isSaving = true, hasSaveFailed = false) }
        viewModelScope.launch {
            runCatching { vehicleRepository.register(registration) }
                .onSuccess { vehicleId ->
                    // 등록되면 바로 홈으로 넘어가 이 화면이 사라지므로, 사진 가져오기와 배경 지우기는 뒤에서 잇는다.
                    state.photoUri?.let {
                        photoRepository.replaceInBackground(vehicleId, it, state.removePhotoBackground)
                    }
                    analytics.track(AnalyticsEvent.ManualRegistrationUsed)
                    analytics.track(
                        AnalyticsEvent.OnboardingCompleted(
                            withPlate = registration.plate != null,
                            knownServiceCount = registration.knownServices.size,
                        ),
                    )
                }
                .onFailure { _uiState.update { it.copy(isSaving = false, hasSaveFailed = true) } }
        }
    }

    private fun draft(state: OnboardingUiState, year: Int) = VehicleRegistration(
        maker = state.maker,
        model = state.model,
        modelYear = year,
        fuelType = state.fuelType ?: FuelType.Gasoline,
        currentMileage = Kilometers(0),
        plate = (PlateNumber.parse(state.plate) as? PlateParseResult.Valid)?.plate,
    )

    private fun goBack() {
        val previous = OnboardingStep.entries.getOrNull(_uiState.value.step.ordinal - 1) ?: return
        moveTo(previous)
    }

    private fun moveTo(step: OnboardingStep) {
        _uiState.update { it.copy(step = step, errors = emptyMap(), quickServiceErrors = emptyMap()) }
        draftStore.save(_uiState.value)
    }

    private fun edit(field: OnboardingField?, transform: OnboardingUiState.() -> OnboardingUiState) {
        _uiState.update { state ->
            state.transform().copy(errors = field?.let { state.errors - it } ?: state.errors, hasSaveFailed = false)
        }
        draftStore.save(_uiState.value)
    }

    private fun editQuickService(item: MaintenanceItem, transform: QuickServiceInput.() -> QuickServiceInput) {
        _uiState.update { state ->
            val current = state.quickServices[item] ?: QuickServiceInput()
            state.copy(
                quickServices = state.quickServices + (item to current.transform()),
                quickServiceErrors = state.quickServiceErrors - item,
            )
        }
        draftStore.save(_uiState.value)
    }

    private fun setErrors(errors: Map<OnboardingField, FieldError>) {
        _uiState.update { it.copy(errors = errors) }
    }

    private companion object {
        const val YEAR_DIGITS = 4
        const val MILEAGE_DIGITS = 7
        const val MAX_MILEAGE_KM = 2_000_000L
    }
}
