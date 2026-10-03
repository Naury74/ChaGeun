package com.naury.chageun.feature.history.form

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class FuelFormContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var total: String? = null
    private var station: String? = null
    private var fullTank: Boolean? = null
    private val actions = FuelFormActions(
        onDateSelected = {},
        onMileageChanged = {},
        onTotalChanged = { total = it },
        onVolumeChanged = {},
        onUnitPriceChanged = {},
        onFullTankChanged = { fullTank = it },
        onStationChanged = { station = it },
        onMemoChanged = {},
        onSave = {},
        onCancel = {},
    )

    private val base = FuelFormUiState(date = LocalDate.of(2026, 10, 1), mileage = "42891")

    @Test
    fun picks_fillAmountStationAndTank() {
        composeRule.setContent { ChageunTheme { FuelFormContent(base, actions) } }

        composeRule.onNodeWithText("₩50,000").performClick()
        composeRule.onNodeWithText("GS Caltex").performClick()
        composeRule.onNodeWithText("Partial").performClick()

        assertThat(total).isEqualTo("50000")
        assertThat(station).isEqualTo("GS Caltex")
        assertThat(fullTank).isFalse()
    }

    @Test
    fun otherStation_revealsNameField() {
        composeRule.setContent { ChageunTheme { FuelFormContent(base, actions) } }

        composeRule.onNodeWithText("Other").performClick()

        composeRule.onNodeWithText("Station name").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_form() {
        val state = base.copy(total = "50000", unitPrice = "1700", stationName = "S-OIL")
        composeRule.setContent { AppFrame { FuelFormContent(state, actions) } }
        composeRule.captureScreen("fuel_form_phone")
    }
}
