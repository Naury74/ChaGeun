package com.naury.chageun.feature.onboarding

import com.naury.chageun.core.model.FuelType

enum class OnboardingStep { Intro, Plate, VehicleInfo, Mileage }

enum class FieldError { Required, InvalidPlate, UnsupportedPlate, InvalidYear, InvalidMileage }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Intro,
    val plate: String = "",
    val maker: String = "",
    val model: String = "",
    val modelYear: String = "",
    val fuelType: FuelType? = null,
    val mileage: String = "",
    val errors: Map<OnboardingField, FieldError> = emptyMap(),
    val isSaving: Boolean = false,
    val hasSaveFailed: Boolean = false,
) {
    val canGoBack: Boolean get() = step != OnboardingStep.Intro && !isSaving
}

enum class OnboardingField { Plate, Maker, Model, ModelYear, FuelType, Mileage }

sealed interface OnboardingAction {
    data object Start : OnboardingAction
    data object Back : OnboardingAction
    data class PlateChanged(val value: String) : OnboardingAction
    data object SubmitPlate : OnboardingAction
    data object SkipPlate : OnboardingAction
    data class MakerChanged(val value: String) : OnboardingAction
    data class ModelChanged(val value: String) : OnboardingAction
    data class ModelYearChanged(val value: String) : OnboardingAction
    data class FuelTypeSelected(val value: FuelType) : OnboardingAction
    data object SubmitVehicleInfo : OnboardingAction
    data class MileageChanged(val value: String) : OnboardingAction
    data object Finish : OnboardingAction
}
