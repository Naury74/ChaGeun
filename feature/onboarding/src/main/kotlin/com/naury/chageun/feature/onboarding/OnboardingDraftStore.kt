package com.naury.chageun.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import java.time.LocalDate

/**
 * 구성 변경과 프로세스 종료 후에도 온보딩 입력 초안을 유지한다.
 *
 * SECURITY: 번호판 초안은 이 프로세스의 saved state에만 두며 로그에 남기지 않는다.
 */
internal class OnboardingDraftStore(private val handle: SavedStateHandle) {

    fun save(state: OnboardingUiState) {
        handle[KEY_STEP] = state.step.name
        handle[KEY_PLATE] = state.plate
        handle[KEY_MAKER] = state.maker
        handle[KEY_MODEL] = state.model
        handle[KEY_YEAR] = state.modelYear
        handle[KEY_FUEL] = state.fuelType?.name
        handle[KEY_MILEAGE] = state.mileage
        handle[KEY_PHOTO] = state.photoUri
        handle[KEY_PHOTO_CUTOUT] = state.removePhotoBackground
        state.quickServices.forEach { (item, input) ->
            handle[quickKey(item, "mode")] = input.mode.name
            handle[quickKey(item, "date")] = input.date?.toEpochDay()
            handle[quickKey(item, "mileage")] = input.mileage
        }
    }

    fun restore() = OnboardingUiState(
        step = handle.get<String>(KEY_STEP)?.let(OnboardingStep::valueOf) ?: OnboardingStep.Intro,
        plate = handle[KEY_PLATE] ?: "",
        maker = handle[KEY_MAKER] ?: "",
        model = handle[KEY_MODEL] ?: "",
        modelYear = handle[KEY_YEAR] ?: "",
        fuelType = handle.get<String>(KEY_FUEL)?.let(FuelType::valueOf),
        mileage = handle[KEY_MILEAGE] ?: "",
        photoUri = handle[KEY_PHOTO],
        removePhotoBackground = handle[KEY_PHOTO_CUTOUT] ?: true,
        quickServices = QUICK_SERVICE_ITEMS.associateWith(::restoreQuickService),
    )

    private fun restoreQuickService(item: MaintenanceItem) = QuickServiceInput(
        mode = handle.get<String>(quickKey(item, "mode"))?.let(QuickServiceMode::valueOf) ?: QuickServiceMode.Unknown,
        date = handle.get<Long>(quickKey(item, "date"))?.let(LocalDate::ofEpochDay),
        mileage = handle[quickKey(item, "mileage")] ?: "",
    )

    private fun quickKey(item: MaintenanceItem, field: String) = "onboarding_quick_${item.name}_$field"

    private companion object {
        const val KEY_STEP = "onboarding_step"
        const val KEY_PLATE = "onboarding_plate"
        const val KEY_MAKER = "onboarding_maker"
        const val KEY_MODEL = "onboarding_model"
        const val KEY_YEAR = "onboarding_year"
        const val KEY_FUEL = "onboarding_fuel"
        const val KEY_MILEAGE = "onboarding_mileage"
        const val KEY_PHOTO = "onboarding_photo"
        const val KEY_PHOTO_CUTOUT = "onboarding_photo_cutout"
    }
}
