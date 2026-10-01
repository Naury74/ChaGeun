package com.naury.chageun.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.VehicleHealthLevel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: HomeUiState) = composeRule.setContent {
        ChageunTheme { HomeScreen(uiState = state, paneCount = 1, onRecordService = {}) }
    }

    @Test
    fun showsHeroWithMileage() {
        show(content(VehicleHealthLevel.Good, emptyList(), status(MaintenanceItem.EngineOil, MaintenanceState.Good)))

        composeRule.onNodeWithText("KG Mobility Torres").assertIsDisplayed()
        composeRule.onNodeWithText("42,180 km").assertIsDisplayed()
        composeRule.onNodeWithText("Mileage as of today").assertIsDisplayed()
    }

    @Test
    fun goodState_showsNothingToCheck() {
        show(content(VehicleHealthLevel.Good, emptyList(), status(MaintenanceItem.EngineOil, MaintenanceState.Good)))

        composeRule.onNodeWithText("Nothing needs attention right now").assertIsDisplayed()
        composeRule.onNodeWithText("Check now").assertDoesNotExist()
    }

    @Test
    fun criticalState_listsOverdueItemsWithDistance() {
        show(
            content(
                VehicleHealthLevel.NeedsAttention,
                listOf(
                    HealthReason.SafetyItemOverdue(MaintenanceItem.Tire),
                    HealthReason.ItemOverdue(MaintenanceItem.EngineOil),
                ),
                status(MaintenanceItem.Tire, MaintenanceState.Overdue, remainingKm = -1_200),
                status(MaintenanceItem.EngineOil, MaintenanceState.Due, remainingKm = 420),
            ),
        )

        composeRule.onNodeWithText("2 items need attention").assertIsDisplayed()
        composeRule.onNodeWithText("1,200 km past due").assertIsDisplayed()
        composeRule.onNodeWithText("About 420 km left").assertIsDisplayed()
    }

    @Test
    fun unknownState_neverClaimsGood_andExplainsMissingData() {
        show(
            content(
                VehicleHealthLevel.InsufficientData,
                listOf(HealthReason.SafetyItemUnknown(MaintenanceItem.BrakePad)),
                status(MaintenanceItem.BrakePad, MaintenanceState.Unknown, missing = setOf(MissingInput.LastService)),
            ),
        )

        composeRule.onNodeWithText("Check your Brake pads details").assertIsDisplayed()
        composeRule.onNodeWithText("No replacement record yet").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing needs attention right now").assertDoesNotExist()
    }
}
