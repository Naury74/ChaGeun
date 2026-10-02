package com.naury.chageun.feature.onboarding

import androidx.compose.ui.test.junit4.v2.createComposeRule
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
    fun vehicleInfo() = screenshot(
        "onboarding_vehicle",
        OnboardingUiState(step = OnboardingStep.VehicleInfo, maker = "Hyundai", model = "Avante", modelYear = "2022"),
    )

    @Test
    fun quickMaintenance() = screenshot("onboarding_quick", OnboardingUiState(step = OnboardingStep.QuickMaintenance))
}
