package com.naury.chageun.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.naury.chageun.core.designsystem.theme.ChageunSpacing
import com.naury.chageun.core.designsystem.theme.Gutters
import com.naury.chageun.core.domain.greeting.Greeting
import com.naury.chageun.core.domain.greeting.GreetingSituation
import com.naury.chageun.core.domain.weather.WeatherTip
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.KoreaRegion
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSnapshot
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

    private val baseState = content(
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

    /** 처음 설치한 사용자가 보는 홈. 날씨는 위치를 고르기 전이라 "날씨 보기" 카드가 나온다. */
    private val state = baseState.copy(weather = HomeWeather.Setup)

    private val rainyState = baseState.copy(
        greetingName = "나우리",
        greeting = Greeting(GreetingSituation.CommuteMorning, seed = 0),
        weather = HomeWeather.Shown(
            WeatherSnapshot(WeatherCondition.Rain, 17.9, 3.4, false, Instant.EPOCH),
            WeatherLocation.Current,
            WeatherTip.Rain,
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
    fun phoneSignedIn_ko() = capture(
        "home_phone_signed_in_ko",
        paneCount = 1,
        state.copy(greetingName = "나우리", greeting = Greeting(GreetingSituation.Evening, seed = 0)),
    )

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    fun tablet() = capture("home_tablet", paneCount = 3)

    /** 비 오는 출근길: 인사말 아래에 기온·날씨와 빗길 팁, 출처가 나온다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherRain_ko() = capture("home_weather_rain_ko", paneCount = 1, rainyState)

    @Test
    @Config(qualifiers = "ko-w360dp-h800dp-night-hdpi")
    fun weatherSnow_dark_ko() = capture(
        "home_weather_snow_dark_ko",
        paneCount = 1,
        rainyState.copy(
            greeting = Greeting(GreetingSituation.MaintenanceOverdue, seed = 0, number = 1),
            weather = HomeWeather.Shown(
                WeatherSnapshot(WeatherCondition.Snow, -3.0, 2.0, false, Instant.EPOCH),
                WeatherLocation.Region(KoreaRegion.Gangwon),
                WeatherTip.Snow,
            ),
        ),
    )

    /** 설정에서 날씨를 끄면 인사말과 날짜만 남는다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherOff_ko() = capture("home_weather_off_ko", paneCount = 1, baseState.copy(weather = HomeWeather.Off))

    /** 펼친 폴더블 안쪽 화면. 머리말이 넓은 폭에서도 로고·버튼과 날씨 카드가 어색하지 않은지 본다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.FOLD_KO)
    fun fold_ko() = capture("home_fold_ko", paneCount = 1, rainyState, spacing = Gutters.Medium)

    /** 600dp에서는 Rail이 옆에 붙어도 홈은 한 칸이다. 두 칸이면 상세 쪽이 360dp보다 좁아진다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.MEDIUM_KO)
    fun medium_ko() = capture("home_medium_ko", paneCount = 1, spacing = Gutters.Medium)

    /** 840dp부터는 차량·상태 요약과 할 일 목록을 나란히 둔다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.EXPANDED_KO)
    fun expanded_ko() = capture("home_expanded_ko", paneCount = 2, rainyState, spacing = Gutters.Expanded)

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

    /** 다가오는 일정 카드. 지난 항목은 오늘에 모여 "지남"으로, 예상 일정은 "예상"으로 보인다. */
    @Test
    @Config(qualifiers = "ko-w360dp-h1800dp-hdpi")
    fun upcomingSchedule_ko() = capture(
        "home_schedule_ko",
        paneCount = 1,
        state.copy(
            upcomingEvents = listOf(
                UpcomingEvent.MaintenanceDue(MaintenanceItem.Tire, TODAY, false, null, isOverdue = true),
                UpcomingEvent.InspectionDue(TODAY.plusDays(6)),
                UpcomingEvent.MaintenanceDue(MaintenanceItem.EngineOil, TODAY.plusDays(11), true, Confidence.Medium),
            ),
        ),
    )
}
