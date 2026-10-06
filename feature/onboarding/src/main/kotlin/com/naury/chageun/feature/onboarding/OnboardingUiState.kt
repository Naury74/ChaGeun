package com.naury.chageun.feature.onboarding

import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import java.time.LocalDate

enum class OnboardingStep { Intro, Plate, VehicleInfo, Photo, Mileage, QuickMaintenance, Notifications }

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
    /** 대표 사진으로 쓸 이미지. 사진 편집기가 앱 캐시에 남긴 파일이라 프로세스가 다시 떠도 읽을 수 있다. */
    val photoUri: String? = null,
    val mileage: String = "",
    /** 계기판을 볼 수 없을 때 고를 수 있는 연식 기준 대략값. 연식을 모르면 null이다. */
    val mileageEstimate: Long? = null,
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
    data class PhotoPicked(val uri: String) : OnboardingAction
    data object RemovePhoto : OnboardingAction
    data object SubmitPhoto : OnboardingAction
    data class MileageChanged(val value: String) : OnboardingAction
    data object SubmitMileage : OnboardingAction
    data class QuickServiceModeSelected(val item: MaintenanceItem, val mode: QuickServiceMode) : OnboardingAction
    data class QuickServiceDateSelected(val item: MaintenanceItem, val date: LocalDate) : OnboardingAction
    data class QuickServiceMileageChanged(val item: MaintenanceItem, val value: String) : OnboardingAction
    data object SubmitQuickMaintenance : OnboardingAction

    /** 알림 권한 요청이 끝난 뒤, 또는 건너뛰면 즉시 보낸다. */
    data object Finish : OnboardingAction
}
