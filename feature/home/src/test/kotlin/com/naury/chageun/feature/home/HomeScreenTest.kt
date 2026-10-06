package com.naury.chageun.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.ui.Hinge
import com.naury.chageun.core.uitesting.assertNoClippedText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: HomeUiState) = composeRule.setContent {
        ChageunTheme { HomeScreen(uiState = state, paneCount = 1, actions = HomeActions({}, {}, {})) }
    }

    @Test
    fun showsHeroWithMileage() {
        show(content(VehicleHealthLevel.Good, emptyList(), status(MaintenanceItem.EngineOil, MaintenanceState.Good)))

        composeRule.onNodeWithText("KG Mobility Torres").assertIsDisplayed()
        composeRule.onNodeWithText("42,180 km").assertIsDisplayed()
        composeRule.onNodeWithText("Mileage as of today").assertIsDisplayed()
    }

    @Test
    fun longMissingList_showsFirstThree_thenExpands() {
        val items = listOf(
            MaintenanceItem.BrakePad,
            MaintenanceItem.BrakeFluid,
            MaintenanceItem.Tire,
            MaintenanceItem.Coolant,
            MaintenanceItem.Wiper,
        )
        show(
            content(
                VehicleHealthLevel.InsufficientData,
                emptyList(),
                *items.map { status(it, MaintenanceState.Unknown, missing = setOf(MissingInput.LastService)) }
                    .toTypedArray(),
            ),
        )

        composeRule.onNodeWithText("Tires").assertIsDisplayed()
        composeRule.onNodeWithText("Wipers").assertDoesNotExist()
        composeRule.onNodeWithText("Show 2 more").performClick()
        composeRule.onNodeWithText("Wipers").assertIsDisplayed()
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

    @Test
    fun showsMileagePrompt_andRecentRecordsEmptyState_thenOpensHistory() {
        var opened = false
        val stale = content(
            VehicleHealthLevel.Good,
            emptyList(),
            status(MaintenanceItem.EngineOil, MaintenanceState.Good),
        )
            .let {
                it.copy(
                    overview = it.overview.copy(
                        currentMileage = it.overview.currentMileage?.copy(date = TODAY.minusDays(45)),
                    ),
                )
            }
        composeRule.setContent {
            ChageunTheme {
                HomeScreen(uiState = stale, paneCount = 1, actions = HomeActions({}, {}, { opened = true }))
            }
        }

        composeRule.onNodeWithText("Tell us your current mileage").assertIsDisplayed()
        composeRule.onNodeWithText("Add your first record").performClick()

        assertThat(opened).isTrue()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsEveryTextVisible() {
        show(
            content(
                VehicleHealthLevel.NeedsAttention,
                listOf(HealthReason.SafetyItemOverdue(MaintenanceItem.Tire)),
                status(MaintenanceItem.Tire, MaintenanceState.Overdue, remainingKm = -1_200),
                status(MaintenanceItem.EngineOil, MaintenanceState.Upcoming, remainingKm = 2_400, remainingDays = 40),
                status(MaintenanceItem.BrakePad, MaintenanceState.Unknown, missing = setOf(MissingInput.LastService)),
            ),
        )

        composeRule.assertNoClippedText()
    }

    @Test
    fun overdueInspection_isListedFirst_andOpensMyCar() {
        var opened = false
        val base = content(
            VehicleHealthLevel.NeedsAttention,
            listOf(HealthReason.InspectionOverdue),
            status(MaintenanceItem.EngineOil, MaintenanceState.Good),
        )
        val inspection = InspectionStatus(
            InspectionSchedule(TODAY.minusDays(3), InspectionSource.User),
            daysLeft = -3,
            state = InspectionState.Overdue,
        )
        composeRule.setContent {
            ChageunTheme {
                HomeScreen(
                    uiState = base.copy(overview = base.overview.copy(inspection = inspection)),
                    paneCount = 1,
                    actions = HomeActions({}, {}, {}, onOpenInspection = { opened = true }),
                )
            }
        }

        composeRule.onNodeWithText("1 item needs attention").assertIsDisplayed()
        composeRule.onNodeWithText("3 days overdue").assertIsDisplayed()
        composeRule.onNodeWithText("Vehicle inspection").performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun dueSoonInspection_headline_namesInspection() {
        show(content(VehicleHealthLevel.Upcoming, listOf(HealthReason.InspectionDueSoon)))

        composeRule.onNodeWithText("Vehicle inspection is coming up").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h700dp-mdpi")
    fun tabletop_keepsCarAboveHinge_andListsBelow() {
        composeRule.setContent {
            ChageunTheme {
                HomeScreen(
                    uiState = content(
                        VehicleHealthLevel.NeedsAttention,
                        listOf(HealthReason.ItemOverdue(MaintenanceItem.EngineOil)),
                        status(MaintenanceItem.EngineOil, MaintenanceState.Overdue, remainingKm = -300),
                    ),
                    paneCount = 2,
                    actions = HomeActions({}, {}, {}),
                    hinge = Hinge(start = 350f, end = 350f, isVertical = false),
                )
            }
        }

        assertThat(composeRule.onNodeWithText("KG Mobility Torres").getBoundsInRoot().bottom.value).isAtMost(342f)
        assertThat(composeRule.onNodeWithText("Check now").getBoundsInRoot().top.value).isAtLeast(358f)
    }
}
