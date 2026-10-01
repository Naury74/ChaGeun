package com.naury.chageun.feature.onboarding

import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import java.time.LocalDate

enum class OnboardingStep { Intro, Plate, VehicleInfo, Mileage, QuickMaintenance, Notifications }

enum class FieldError {
    Required,
    InvalidPlate,
    UnsupportedPlate,
    InvalidYear,
    InvalidMileage,
    FutureDate,
    ExceedsCurrentMileage,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Intro,
    val plate: String = "",
    val maker: String = "",
    val model: String = "",
    val modelYear: String = "",
    val fuelType: FuelType? = null,
    val mileage: String = "",
    val quickServices: Map<MaintenanceItem, QuickServiceInput> = QUICK_SERVICE_ITEMS.associateWith {
        QuickServiceInput()
    },
    val errors: Map<OnboardingField, FieldError> = emptyMap(),
    val quickServiceErrors: Map<MaintenanceItem, FieldError> = emptyMap(),
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
    data object SubmitMileage : OnboardingAction
    data class QuickServiceModeSelected(val item: MaintenanceItem, val mode: QuickServiceMode) : OnboardingAction
    data class QuickServiceDateSelected(val item: MaintenanceItem, val date: LocalDate) : OnboardingAction
    data class QuickServiceMileageChanged(val item: MaintenanceItem, val value: String) : OnboardingAction
    data object SubmitQuickMaintenance : OnboardingAction

    /** Sent after the notification permission prompt resolves, or immediately when skipped. */
    data object Finish : OnboardingAction
}
