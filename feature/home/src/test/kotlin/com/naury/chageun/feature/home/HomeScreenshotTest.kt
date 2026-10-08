package com.naury.chageun.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.naury.chageun.core.designsystem.theme.ChageunSpacing
import com.naury.chageun.core.designsystem.theme.Gutters
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

    private fun capture(
        name: String,
        paneCount: Int,
        uiState: HomeUiState.Content = state,
        spacing: ChageunSpacing = Gutters.Compact,
    ) {
        composeRule.setContent {
            AppFrame(spacing) {
                HomeScreen(uiState = uiState, paneCount = paneCount, actions = HomeActions({}, {}, {}, onAddPhoto = {}))
            }
        }
        composeRule.captureScreen(name)
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun phone() = capture("home_phone", paneCount = 1)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun phoneDark() = capture("home_phone_dark", paneCount = 1)

    /** 로그인한 사용자는 이름으로 인사하고 계정 버튼에 첫 글자를 보여 준다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun phoneSignedIn_ko() =
        capture("home_phone_signed_in_ko", paneCount = 1, state.copy(greetingName = "나우리", dayPart = DayPart.Evening))

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    fun tablet() = capture("home_tablet", paneCount = 3)

    /** 600dp에서는 Rail이 옆에 붙어도 홈은 한 칸이다. 두 칸이면 상세 쪽이 360dp보다 좁아진다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.MEDIUM_KO)
    fun medium_ko() = capture("home_medium_ko", paneCount = 1, spacing = Gutters.Medium)

    /** 840dp부터는 차량·상태 요약과 할 일 목록을 나란히 둔다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.EXPANDED_KO)
    fun expanded_ko() = capture("home_expanded_ko", paneCount = 2, spacing = Gutters.Expanded)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun good_ko() = capture("home_good_ko", paneCount = 1, goodState)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun critical_ko() = capture("home_critical_ko", paneCount = 1, criticalState)

    /** 기록이 모자라면 정상으로 보이지 않고, 무엇을 넣어야 하는지 먼저 보여 준다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun unknown_ko() = capture("home_unknown_ko", paneCount = 1, unknownState)

    private val goodState = content(
        VehicleHealthLevel.Good,
        emptyList(),
        status(MaintenanceItem.EngineOil, MaintenanceState.Good, remainingKm = 7_800, remainingDays = 240),
        status(MaintenanceItem.Tire, MaintenanceState.Good, remainingKm = 21_000),
        status(MaintenanceItem.BrakePad, MaintenanceState.Good, remainingKm = 15_400),
        status(MaintenanceItem.Battery, MaintenanceState.Good, remainingDays = 400),
    ).copy(recentRecords = state.recentRecords)

    private val criticalState = content(
        VehicleHealthLevel.NeedsAttention,
        listOf(
            HealthReason.SafetyItemOverdue(MaintenanceItem.BrakePad),
            HealthReason.ItemOverdue(MaintenanceItem.EngineOil),
            HealthReason.ItemDueSoon(MaintenanceItem.Tire),
        ),
        status(MaintenanceItem.BrakePad, MaintenanceState.Overdue, remainingKm = -2_300),
        status(MaintenanceItem.EngineOil, MaintenanceState.Overdue, remainingKm = -600, remainingDays = -14),
        status(MaintenanceItem.Tire, MaintenanceState.Due, remainingKm = 300),
        status(MaintenanceItem.Battery, MaintenanceState.Good, remainingDays = 400),
    ).copy(recentRecords = state.recentRecords)

    private val unknownState = content(
        VehicleHealthLevel.InsufficientData,
        listOf(
            HealthReason.SafetyItemUnknown(MaintenanceItem.BrakePad),
            HealthReason.SafetyItemUnknown(MaintenanceItem.Tire),
            HealthReason.ItemUnknown(MaintenanceItem.EngineOil),
        ),
        status(MaintenanceItem.BrakePad, MaintenanceState.Unknown, missing = setOf(MissingInput.LastService)),
        status(MaintenanceItem.Tire, MaintenanceState.Unknown, missing = setOf(MissingInput.LastService)),
        status(MaintenanceItem.EngineOil, MaintenanceState.Unknown, missing = setOf(MissingInput.LastServiceMileage)),
    )
}
