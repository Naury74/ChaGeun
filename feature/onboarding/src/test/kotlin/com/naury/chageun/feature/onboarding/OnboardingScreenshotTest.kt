package com.naury.chageun.feature.onboarding

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = ScreenshotDevices.PHONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun screenshot(name: String, state: OnboardingUiState) {
        composeRule.setContent { AppFrame { OnboardingScreen(uiState = state, onAction = {}) } }
        composeRule.captureScreen(name)
    }

    @Test
    fun intro() = screenshot("onboarding_intro", OnboardingUiState())

    @Test
    fun plate() = screenshot("onboarding_plate", OnboardingUiState(step = OnboardingStep.Plate, plate = "123가4567"))

    @Test
    fun vehicleInfo() = screenshot(
        "onboarding_vehicle",
        OnboardingUiState(
            step = OnboardingStep.VehicleInfo,
            maker = "Hyundai",
            model = "Avante",
            modelYear = "2022",
            fuelType = FuelType.Gasoline,
        ),
    )

    @Test
    fun mileage() = screenshot(
        "onboarding_mileage",
        OnboardingUiState(
            step = OnboardingStep.Mileage,
            mileage = "42180",
            modelYear = "2022",
            mileageEstimate = 57_000,
        ),
    )

    @Test
    fun notifications() = screenshot("onboarding_notifications", OnboardingUiState(step = OnboardingStep.Notifications))

    @Test
    fun quickMaintenance() = screenshot(
        "onboarding_quick",
        OnboardingUiState(
            step = OnboardingStep.QuickMaintenance,
            quickServices = QUICK_SERVICE_ITEMS.associateWith { QuickServiceInput() } +
                (MaintenanceItem.EngineOil to QuickServiceInput(QuickServiceMode.HalfYear)),
        ),
    )

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun introKorean() = screenshot("onboarding_intro_ko", OnboardingUiState())

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun quickMaintenanceKorean() = screenshot(
        "onboarding_quick_ko",
        OnboardingUiState(
            step = OnboardingStep.QuickMaintenance,
            quickServices = QUICK_SERVICE_ITEMS.associateWith { QuickServiceInput() } +
                (MaintenanceItem.EngineOil to QuickServiceInput(QuickServiceMode.HalfYear)),
        ),
    )
}
