package com.naury.chageun.core.ui

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.FuelType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class VehicleInfoFieldsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(maker: String, model: String) = composeRule.setContent {
        ChageunTheme {
            VehicleInfoFields(
                maker = maker,
                model = model,
                modelYear = "2023",
                fuelType = FuelType.Gasoline,
                onMakerChanged = {},
                onModelChanged = {},
                onModelYearChanged = {},
                onFuelTypeSelected = {},
            )
        }
    }

    @Test
    fun selectsCatalogChips_forNamesSavedInAnotherLanguage() {
        show(maker = "Kia", model = "Sportage")

        composeRule.onNodeWithText("기아").assertIsSelected()
        composeRule.onNodeWithText("스포티지").assertIsSelected()
        composeRule.onNodeWithText("기타").assertIsNotSelected()
    }

    @Test
    fun fallsBackToTypedName_whenNoLanguageMatches() {
        show(maker = "Lotus", model = "Elise")

        composeRule.onNodeWithText("기타").assertIsSelected()
        composeRule.onNodeWithText("Lotus").assertExists()
    }
}
