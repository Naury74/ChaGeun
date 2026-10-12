package com.naury.chageun.feature.home

import androidx.compose.runtime.CompositionLocalProvider
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
import com.naury.chageun.core.uitesting.SKY_COLOR_TOLERANCE
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

    /** 처음 설치한 사용자가 보는 홈. 날씨는 위치를 고르기 전이라 "날씨 맞춤 운전 팁 켜기" 알약이 나온다. */
    private val state = baseState.copy(weather = HomeWeather.Setup)

    private fun weatherState(
        scene: WeatherScene,
        condition: WeatherCondition,
        temperatureC: Double,
        tip: WeatherTip,
        location: WeatherLocation = WeatherLocation.Region(KoreaRegion.Seoul),
        greeting: Greeting = Greeting(GreetingSituation.CommuteMorning, seed = 0),
    ) = baseState.copy(
        greetingName = "나우리",
        greeting = greeting,
        weather = HomeWeather.Shown(
            WeatherSnapshot(condition, temperatureC, 3.0, scene == WeatherScene.ClearNight, Instant.EPOCH),
            location,
            tip,
            scene,
        ),
    )

    private val rainyState = weatherState(
        WeatherScene.Rain,
        WeatherCondition.Rain,
        17.9,
        WeatherTip.Rain,
        location = WeatherLocation.Current,
    )
    private val clearState =
        weatherState(WeatherScene.ClearDay, WeatherCondition.Clear, 22.0, WeatherTip.NiceDay)
    private val nightState = weatherState(
        WeatherScene.ClearNight,
        WeatherCondition.Clear,
        14.0,
        WeatherTip.Calm,
        greeting = Greeting(GreetingSituation.Evening, seed = 0),
    )
    private val cloudyState = weatherState(
        WeatherScene.Cloudy,
        WeatherCondition.Cloudy,
        27.0,
        WeatherTip.Calm,
        greeting = Greeting(GreetingSituation.AutumnAfternoon, seed = 0),
    )
    private val snowState = weatherState(
        WeatherScene.Snow,
        WeatherCondition.Snow,
        -3.0,
        WeatherTip.Snow,
        location = WeatherLocation.Region(KoreaRegion.Gangwon),
        greeting = Greeting(GreetingSituation.MaintenanceOverdue, seed = 0, number = 1),
    )
    private val fogState = weatherState(
        WeatherScene.Fog,
        WeatherCondition.Fog,
        9.0,
        WeatherTip.Fog,
        location = WeatherLocation.Region(KoreaRegion.Incheon),
    )

    private fun capture(
        name: String,
        paneCount: Int,
        uiState: HomeUiState.Content = state,
        spacing: ChageunSpacing = Gutters.Compact,
    ) {
        composeRule.setContent {
            // 하늘 애니메이션은 정해진 시각의 한 장면으로 찍는다.
            CompositionLocalProvider(LocalFrozenSkyTime provides SKY_FRAME_MS) {
                AppFrame(spacing) {
                    HomeScreen(
                        uiState = uiState,
                        paneCount = paneCount,
                        actions = HomeActions({}, {}, {}, onAddPhoto = {}),
                    )
                }
            }
        }
        // 날씨 하늘의 흐림·빗줄기는 OS마다 1~2단계 다르게 그려진다.
        composeRule.captureScreen(name, colorTolerance = SKY_COLOR_TOLERANCE)
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
    fun tablet() = capture("home_tablet", paneCount = 3, clearState)

    /** 세 칸에서도 하늘은 왼쪽 칸 머리에만 깔리고 오른쪽으로 갈수록 흐려진다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.LARGE_KO)
    fun largeRain_ko() = capture("home_large_rain_ko", paneCount = 3, rainyState, spacing = Gutters.Expanded)

    /** 비 오는 출근길: 차가운 회청색 하늘에 가는 빗줄기, 인사말 아래 날씨 한 줄. */
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherRain_ko() = capture("home_weather_rain_ko", paneCount = 1, rainyState)

    @Test
    @Config(qualifiers = DARK_KO)
    fun weatherRain_dark_ko() = capture("home_weather_rain_dark_ko", paneCount = 1, rainyState)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherClear_ko() = capture("home_weather_clear_ko", paneCount = 1, clearState)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherNight_ko() = capture("home_weather_night_ko", paneCount = 1, nightState)

    @Test
    @Config(qualifiers = DARK_KO)
    fun weatherNight_dark_ko() = capture("home_weather_night_dark_ko", paneCount = 1, nightState)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherCloudy_ko() = capture("home_weather_cloudy_ko", paneCount = 1, cloudyState)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherSnow_ko() = capture("home_weather_snow_ko", paneCount = 1, snowState)

    @Test
    @Config(qualifiers = DARK_KO)
    fun weatherSnow_dark_ko() = capture("home_weather_snow_dark_ko", paneCount = 1, snowState)

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherFog_ko() = capture("home_weather_fog_ko", paneCount = 1, fogState)

    /** 날씨를 아직 안 켰을 때의 작은 켜기 알약과 닫기 버튼. */
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherSetup_ko() = capture("home_weather_setup_ko", paneCount = 1)

    /** 설정에서 날씨를 끄면 인사말과 날짜만 남는다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun weatherOff_ko() = capture("home_weather_off_ko", paneCount = 1, baseState.copy(weather = HomeWeather.Off))

    /** 펼친 폴더블 안쪽 화면. 넓은 폭에서도 하늘과 날씨 줄이 어색하지 않은지 본다. */
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

    private companion object {
        const val DARK_KO = "ko-w360dp-h800dp-night-hdpi"
        const val SKY_FRAME_MS = 2_400L
    }
}
