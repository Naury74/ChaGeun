package com.naury.chageun.feature.home

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.greeting.Greeting
import com.naury.chageun.core.domain.greeting.GreetingSituation
import com.naury.chageun.core.domain.weather.WeatherTip
import com.naury.chageun.core.model.KoreaRegion
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSnapshot
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class HomeHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val rain = HomeWeather.Shown(
        WeatherSnapshot(WeatherCondition.Rain, 17.6, 3.4, false, Instant.EPOCH),
        WeatherLocation.Region(KoreaRegion.Busan),
        WeatherTip.Rain,
    )

    private fun show(
        weather: HomeWeather = HomeWeather.Off,
        greeting: Greeting = Greeting(GreetingSituation.Morning, seed = 0),
        name: String? = null,
        onSetUpWeather: () -> Unit = {},
    ) = composeRule.setContent {
        ChageunTheme {
            HomeScreen(
                uiState = content(VehicleHealthLevel.Good, emptyList())
                    .copy(weather = weather, greeting = greeting, greetingName = name),
                paneCount = 1,
                actions = HomeActions({}, {}, {}, onSetUpWeather = onSetUpWeather),
            )
        }
    }

    @Test
    fun brandTitle_isOneHeading_readOnce() {
        show()

        // 로고 그림은 읽지 않고, 이름만 제목으로 한 번 읽는다.
        composeRule.onAllNodesWithText("Chageun").assertCountEquals(1)
        composeRule.onNode(hasText("Chageun") and isHeading()).assertIsDisplayed()
        composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription) and hasText("Chageun"),
        )
            .assertCountEquals(0)
    }

    @Test
    fun greeting_picksVariantByDay_andFillsNumberAndName() {
        show(greeting = Greeting(GreetingSituation.Morning, seed = 1), name = "Naury")

        composeRule.onNode(hasText("Fresh morning, Naury") and isHeading()).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h2000dp")
    fun greeting_ko_putsCountIntoMessage() {
        show(greeting = Greeting(GreetingSituation.MaintenanceOverdue, seed = 0, number = 2), name = "나우리")

        composeRule.onNodeWithText("나우리님, 밀린 정비가 2개 있어요").assertIsDisplayed()
    }

    @Test
    fun weatherSetup_opensLocationPicker() {
        var opened = 0
        show(weather = HomeWeather.Setup, onSetUpWeather = { opened++ })

        composeRule.onNode(hasText("Show weather", substring = true) and hasClickAction()).performClick()

        assertThat(opened).isEqualTo(1)
        composeRule.assertAccessibleControls()
    }

    @Test
    fun weatherNeedsPermission_offersToChooseAgain() {
        var opened = 0
        show(weather = HomeWeather.NeedsPermission, onSetUpWeather = { opened++ })

        composeRule.onNodeWithText("Location permission is off").assertIsDisplayed()
        composeRule.onNode(hasText("Choose location", substring = true) and hasClickAction()).performClick()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun weatherLoading_showsProgressText() {
        show(weather = HomeWeather.Loading)

        composeRule.onNodeWithText("Checking the weather…").assertIsDisplayed()
    }

    @Test
    fun weatherShown_hasTemperatureTipAndAttribution() {
        show(weather = rain)

        composeRule.onNodeWithText("18° Rain · Busan").assertIsDisplayed()
        composeRule.onNodeWithText("Wet roads, slow down 20% and keep your distance").assertIsDisplayed()
        composeRule.onNodeWithText("Weather: MET Norway").assertIsDisplayed()
    }

    @Test
    fun weatherOffOrUnavailable_showsNoCard() {
        show(weather = HomeWeather.Unavailable)

        composeRule.onAllNodesWithText("Weather: MET Norway").assertCountEquals(0)
        composeRule.onAllNodesWithText("Show weather").assertCountEquals(0)
        composeRule.onAllNodesWithText("Checking the weather…").assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h2000dp", fontScale = 2f)
    fun largeFont_ko_headerAndWeatherFit() {
        show(
            weather = rain.copy(tip = WeatherTip.Heat),
            greeting = Greeting(GreetingSituation.CarAnniversary, seed = 0, number = 3),
            name = "나우리",
        )

        composeRule.assertNoClippedText()
    }

    @Test
    @Config(fontScale = 2f)
    fun largeFont_setupPromptFits() {
        show(weather = HomeWeather.Setup)

        composeRule.assertNoClippedText()
        composeRule.assertAccessibleControls()
    }
}
