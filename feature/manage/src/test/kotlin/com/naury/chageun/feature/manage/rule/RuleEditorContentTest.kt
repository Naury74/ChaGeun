package com.naury.chageun.feature.manage.rule

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
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
@Config(qualifiers = "w360dp-h2000dp")
class RuleEditorContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var km: String? = null
    private var months: String? = null
    private val state = RuleEditorUiState(
        item = MaintenanceItem.EngineOil,
        isLoading = false,
        intervalKm = "10000",
        intervalMonths = "12",
    )

    private fun show() = composeRule.setContent {
        ChageunTheme {
            RuleEditorContent(
                uiState = state,
                onIntervalKmChanged = { km = it },
                onIntervalMonthsChanged = { months = it },
                onEnabledChanged = {},
                onSave = {},
                onReset = {},
                onDismiss = {},
            )
        }
    }

    @Test
    fun picks_fillIntervals() {
        show()

        composeRule.onNodeWithText("5,000 km").performClick()
        composeRule.onNodeWithText("6 months").performClick()

        assertThat(km).isEqualTo("5000")
        assertThat(months).isEqualTo("6")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot() {
        composeRule.setContent {
            AppFrame {
                RuleEditorContent(state, {}, {}, {}, {}, {}, {})
            }
        }
        composeRule.captureScreen("rule_editor_phone")
    }
}
