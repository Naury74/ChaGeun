package com.naury.chageun.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.naury.chageun.core.model.FuelType

/**
 * Keeps the onboarding draft across configuration changes and process death.
 *
 * SECURITY: The plate draft lives only in this process's saved state and is never logged.
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
    }

    fun restore() = OnboardingUiState(
        step = handle.get<String>(KEY_STEP)?.let(OnboardingStep::valueOf) ?: OnboardingStep.Intro,
        plate = handle[KEY_PLATE] ?: "",
        maker = handle[KEY_MAKER] ?: "",
        model = handle[KEY_MODEL] ?: "",
        modelYear = handle[KEY_YEAR] ?: "",
        fuelType = handle.get<String>(KEY_FUEL)?.let(FuelType::valueOf),
        mileage = handle[KEY_MILEAGE] ?: "",
    )

    private companion object {
        const val KEY_STEP = "onboarding_step"
        const val KEY_PLATE = "onboarding_plate"
        const val KEY_MAKER = "onboarding_maker"
        const val KEY_MODEL = "onboarding_model"
        const val KEY_YEAR = "onboarding_year"
        const val KEY_FUEL = "onboarding_fuel"
        const val KEY_MILEAGE = "onboarding_mileage"
    }
}
