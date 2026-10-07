package com.naury.chageun.feature.vehicle.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.vehicle.RegistrationError
import com.naury.chageun.core.domain.vehicle.UpdateVehicleUseCase
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.PlateChange
import com.naury.chageun.core.model.PlateNumber
import com.naury.chageun.core.model.PlateParseResult
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleProfileUpdate
import com.naury.chageun.core.ui.VehicleInfoField
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VehicleEditError { Required, InvalidYear, InvalidPlate, UnsupportedPlate }

/** 차량번호 칸. [VehicleInfoField]에 없는 값이라 따로 둔다. */
enum class VehicleEditField { Maker, Model, ModelYear, FuelType, Plate }

data class VehicleEditUiState(
    val isLoaded: Boolean = false,
    val maker: String = "",
    val model: String = "",
    val modelYear: String = "",
    val fuelType: FuelType? = null,
    val originalFuelType: FuelType? = null,
    val trim: String = "",
    val currentPlateMasked: String? = null,
    /** 비어 있으면 지금 번호를 그대로 둔다. */
    val newPlate: String = "",
    val isPlateRemoved: Boolean = false,
    val firstRegistrationDate: LocalDate? = null,
    val errors: Map<VehicleEditField, VehicleEditError> = emptyMap(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val isFuelChanged: Boolean get() = originalFuelType != null && fuelType != originalFuelType
}

@HiltViewModel
class VehicleEditViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val updateVehicle: UpdateVehicleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleEditUiState())
    val uiState: StateFlow<VehicleEditUiState> = _uiState.asStateFlow()

    private var vehicle: Vehicle? = null

    init {
        viewModelScope.launch {
            val current = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            vehicle = current
            _uiState.value = VehicleEditUiState(
                isLoaded = true,
                maker = current.maker,
                model = current.model,
                modelYear = current.modelYear?.toString().orEmpty(),
                fuelType = current.fuelType,
                originalFuelType = current.fuelType,
                trim = current.trim.orEmpty(),
                currentPlateMasked = current.plateMasked,
                firstRegistrationDate = current.firstRegistrationDate,
            )
        }
    }

    fun onMakerChanged(value: String) = edit(VehicleEditField.Maker) { copy(maker = value) }

    fun onModelChanged(value: String) = edit(VehicleEditField.Model) { copy(model = value) }

    fun onModelYearChanged(value: String) = edit(VehicleEditField.ModelYear) {
        copy(modelYear = value.filter(Char::isDigit).take(YEAR_DIGITS))
    }

    fun onFuelTypeSelected(value: FuelType) = edit(VehicleEditField.FuelType) { copy(fuelType = value) }

    fun onTrimChanged(value: String) = _uiState.update { it.copy(trim = value) }

    fun onPlateChanged(value: String) = edit(VehicleEditField.Plate) { copy(newPlate = value, isPlateRemoved = false) }

    fun onRemovePlate() = edit(VehicleEditField.Plate) { copy(newPlate = "", isPlateRemoved = true) }

    fun onKeepPlate() = _uiState.update { it.copy(isPlateRemoved = false) }

    fun onFirstRegistrationDateChanged(date: LocalDate?) = _uiState.update { it.copy(firstRegistrationDate = date) }

    fun save() {
        val current = vehicle
        val state = _uiState.value
        if (current == null || state.isSaving) return
        val errors = validate(state)
        val update = toUpdate(state)
        if (errors.isNotEmpty() || update == null) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val rejected = updateVehicle(current, update)
            val yearError = (RegistrationError.ModelYearOutOfRange in rejected)
                .let { if (it) mapOf(VehicleEditField.ModelYear to VehicleEditError.InvalidYear) else emptyMap() }
            _uiState.update { it.copy(isSaving = false, isSaved = rejected.isEmpty(), errors = yearError) }
        }
    }

    private fun validate(state: VehicleEditUiState): Map<VehicleEditField, VehicleEditError> = buildMap {
        if (state.maker.isBlank()) put(VehicleEditField.Maker, VehicleEditError.Required)
        if (state.model.isBlank()) put(VehicleEditField.Model, VehicleEditError.Required)
        if (state.fuelType == null) put(VehicleEditField.FuelType, VehicleEditError.Required)
        if (state.modelYear.toIntOrNull() == null) put(VehicleEditField.ModelYear, VehicleEditError.Required)
        if (plateChange(state) == null) put(VehicleEditField.Plate, plateError(state.newPlate))
    }

    private fun toUpdate(state: VehicleEditUiState): VehicleProfileUpdate? {
        val year = state.modelYear.toIntOrNull()
        val fuelType = state.fuelType
        val plate = plateChange(state)
        if (year == null || fuelType == null || plate == null) return null
        return VehicleProfileUpdate(
            state.maker,
            state.model,
            year,
            fuelType,
            state.trim,
            plate,
            state.firstRegistrationDate,
        )
    }

    /** null이면 입력한 번호가 올바르지 않다는 뜻이다. */
    private fun plateChange(state: VehicleEditUiState): PlateChange? = when {
        state.isPlateRemoved -> PlateChange.Remove
        state.newPlate.isBlank() -> PlateChange.Keep
        else -> (PlateNumber.parse(state.newPlate) as? PlateParseResult.Valid)?.let { PlateChange.Replace(it.plate) }
    }

    private fun plateError(input: String): VehicleEditError =
        if (PlateNumber.parse(input) == PlateParseResult.UnsupportedUsage) {
            VehicleEditError.UnsupportedPlate
        } else {
            VehicleEditError.InvalidPlate
        }

    private fun edit(field: VehicleEditField, change: VehicleEditUiState.() -> VehicleEditUiState) =
        _uiState.update { it.change().copy(errors = it.errors - field) }

    private companion object {
        const val YEAR_DIGITS = 4
    }
}
