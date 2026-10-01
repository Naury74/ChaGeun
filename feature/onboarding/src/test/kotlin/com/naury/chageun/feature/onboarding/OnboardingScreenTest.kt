package com.naury.chageun.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.uitesting.assertNoClippedText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp", fontScale = 2f)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun largeFont_keepsEveryStepReadable() {
        var state by mutableStateOf(OnboardingUiState())
        composeRule.setContent { ChageunTheme { OnboardingScreen(uiState = state, onAction = {}) } }

        OnboardingStep.entries.forEach { step ->
            state = OnboardingUiState(
                step = step,
                errors = mapOf(OnboardingField.Plate to FieldError.UnsupportedPlate),
                hasSaveFailed = true,
            )
            composeRule.waitForIdle()

            composeRule.assertNoClippedText()
        }
    }
}
