package com.naury.chageun.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = content(
        VehicleHealthLevel.NeedsAttention,
        listOf(
            HealthReason.SafetyItemOverdue(MaintenanceItem.Tire),
            HealthReason.ItemDueSoon(MaintenanceItem.EngineOil),
        ),
        status(MaintenanceItem.Tire, MaintenanceState.Overdue, remainingKm = -1_200),
        status(MaintenanceItem.EngineOil, MaintenanceState.Due, remainingKm = 420, remainingDays = 30),
        status(MaintenanceItem.AirFilter, MaintenanceState.Upcoming, remainingKm = 2_400),
        status(MaintenanceItem.BrakePad, MaintenanceState.Unknown, missing = setOf(MissingInput.LastService)),
        status(MaintenanceItem.Battery, MaintenanceState.Good, remainingDays = 400),
    ).copy(
        recentRecords = listOf(
            TimelineItem(
                RecordRef(TimelineEventType.Fuel, "f1"),
                TODAY.minusDays(3),
                "S-Oil",
                null,
                Kilometers(42_000),
                70_000,
                RecordSource.User,
                Instant.EPOCH,
            ),
        ),
    )

    private fun capture(name: String, paneCount: Int) {
        composeRule.setContent {
            AppFrame { HomeScreen(uiState = state, paneCount = paneCount, actions = HomeActions({}, {}, {})) }
        }
        composeRule.captureScreen(name)
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun phone() = capture("home_phone", paneCount = 1)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun phoneDark() = capture("home_phone_dark", paneCount = 1)

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    fun tablet() = capture("home_tablet", paneCount = 3)
}
