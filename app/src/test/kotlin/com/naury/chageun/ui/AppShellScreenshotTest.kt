package com.naury.chageun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleHealth
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.ui.Hinge
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import com.naury.chageun.feature.home.HomeActions
import com.naury.chageun.feature.home.HomeScreen
import com.naury.chageun.feature.home.HomeUiState
import com.naury.chageun.feature.home.homePaneCount
import com.naury.chageun.navigation.TopLevelDestination
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 앱 셸을 창 폭마다 찍는다. 360dp는 하단 바, 600dp부터는 Rail이고, 홈은 840dp부터 두 칸이 된다.
 * 홈 화면은 ViewModel 없이 고정 상태로 그려 Rail 옆에 남는 폭에서 어떻게 놓이는지 본다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppShellScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, hinge: Hinge? = null) {
        composeRule.setContent {
            val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
            AppFrame(spacingFor(windowSizeClass)) {
                Box(Modifier.fillMaxSize()) {
                    ChageunApp(
                        destinationContent = { destination, _ ->
                            if (destination == TopLevelDestination.Home) HomeDestination(windowSizeClass, hinge)
                        },
                    )
                    hinge?.let { FoldLine(it) }
                }
            }
        }
        composeRule.captureScreen(name)
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun phoneBottomBar_ko() = capture("shell_phone_ko")

    @Test
    @Config(qualifiers = ScreenshotDevices.MEDIUM_KO)
    fun mediumRail_ko() = capture("shell_medium_ko")

    @Test
    @Config(qualifiers = ScreenshotDevices.EXPANDED_KO)
    fun expandedRailTwoPanes_ko() = capture("shell_expanded_ko")

    /** Book 자세에서는 Rail 때문에 홈이 창 가운데에서 비켜 있어도 칸 경계가 접힘에 맞춰진다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.FOLD_KO)
    fun foldBook_ko() = capture("shell_fold_book_ko", hinge = Hinge(FOLD_CENTER_PX, FOLD_CENTER_PX, isVertical = true))
}

@Composable
private fun HomeDestination(windowSizeClass: WindowSizeClass, hinge: Hinge?) {
    // HomeRoute처럼 세로 Hinge가 창을 나누면 폭과 상관없이 두 칸으로 접힘 양쪽에 나눈다.
    val paneCount = homePaneCount(windowSizeClass).let { if (hinge != null) maxOf(it, 2) else it }
    HomeScreen(HOME, paneCount, HomeActions({}, {}, {}), hinge = hinge)
}

/** Screenshot에서 접힘 위치를 알아볼 수 있게 Hinge 자리에 선을 그린다. 실제 앱에는 없다. */
@Composable
private fun FoldLine(hinge: Hinge) {
    val x = with(LocalDensity.current) { hinge.start.toDp() }
    Box(
        Modifier
            .offset(x = x - 1.dp)
            .width(2.dp)
            .fillMaxHeight()
            .background(Color.Magenta.copy(alpha = 0.6f)),
    )
}

// FOLD_KO는 792dp × hdpi(1.5)라 창 가운데가 594px이다.
private const val FOLD_CENTER_PX = 594f

private val TODAY = LocalDate.of(2026, 10, 1)

private fun status(
    item: MaintenanceItem,
    state: MaintenanceState,
    remainingKm: Long? = null,
    missing: Set<MissingInput> = emptySet(),
) = MaintenanceStatus(
    item = item,
    state = state,
    remainingKm = remainingKm,
    remainingDays = null,
    distanceDue = null,
    dateDue = null,
    estimatedDue = null,
    missingInputs = missing,
    ruleSource = RuleSource.Generic,
)

private val HOME = HomeUiState.Content(
    vehicle = Vehicle(
        id = VehicleId("v1"),
        maker = "KG Mobility",
        model = "Torres",
        modelYear = 2023,
        trim = null,
        fuelType = FuelType.Gasoline,
        firstRegistrationDate = null,
        displacementCc = null,
        plateMasked = null,
        registrationMode = RegistrationMode.Manual,
        isPrimary = true,
    ),
    overview = MaintenanceOverview(
        statuses = listOf(
            status(MaintenanceItem.Tire, MaintenanceState.Overdue, remainingKm = -1_200),
            status(MaintenanceItem.EngineOil, MaintenanceState.Due, remainingKm = 420),
            status(MaintenanceItem.BrakePad, MaintenanceState.Unknown, missing = setOf(MissingInput.LastService)),
            status(MaintenanceItem.Battery, MaintenanceState.Good),
        ),
        rules = emptyMap(),
        disabledItems = emptyList(),
        health = VehicleHealth(
            VehicleHealthLevel.NeedsAttention,
            listOf(
                HealthReason.SafetyItemOverdue(MaintenanceItem.Tire),
                HealthReason.ItemDueSoon(MaintenanceItem.EngineOil),
            ),
        ),
        currentMileage = MileageReading(TODAY, Kilometers(42_180)),
    ),
    today = TODAY,
)
