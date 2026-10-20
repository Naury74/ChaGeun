package com.naury.chageun.flow

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.core.ui.R as UiR
import com.naury.chageun.di.FixedWeatherRepository
import com.naury.chageun.feature.home.R as HomeR
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 홈 날씨 켜기에서 지역을 시·도 → 시·군·구 → 읍·면·동까지 직접 고르면 홈 날씨 줄에 그 동네가 보이고,
 * 설정 › 날씨에는 시·도부터 이어 쓴 전체 이름이 보인다.
 *
 * 날씨는 [FixedWeatherRepository]가 네트워크 없이 늘 18° 비로 답한다. 비 오는 하늘은 끝없이 움직이므로
 * [ReduceMotionRule]로 '애니메이션 삭제'를 켜 한 장면으로 멈춘다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WeatherRegionFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val reduceMotion = ReduceMotionRule()

    @get:Rule(order = 2)
    val rule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { AppRobot(rule) }

    @Test
    fun pickNeighborhood_showsItOnHome_andFullPathInSettings() {
        robot.registerVehicle()

        robot.clickText(HomeR.string.home_weather_enable)
        // 영어에서는 선택지와 다음 창 제목이 같은 글자라 누를 수 있는 선택지만 고른다.
        robot.click(hasText(robot.text(UiR.string.weather_location_pick_region)) and hasClickAction())
        // 지명은 기기 언어와 상관없이 한국어다. 고른 단계의 이름은 창 제목(누를 수 없음)에도 나오므로 목록 줄만 누른다.
        listOf(PROVINCE, DISTRICT, TOWN).forEach { robot.click(hasText(it) and hasClickAction()) }

        // 날씨 줄은 한 문장으로 읽히도록 내용 설명 하나로 묶여 있다. 홈 맨 위에 있어서, 화면이 짧은 기기(API 26·27 Pixel 2)에서는
        // 지역을 고르는 동안 목록이 내려가 그려지지 않으므로 목록을 거슬러 올라가며 찾는다.
        val temperature = FixedWeatherRepository.TEMPERATURE_C.roundToInt().toString()
        robot.waitForScrolling(
            hasContentDescription(TOWN, substring = true) and
                hasContentDescription(temperature, substring = true) and
                hasContentDescription(robot.text(HomeR.string.home_weather_condition_rain), substring = true),
        )

        robot.openSettingsFromHome()
        robot.scrollTo(hasText("$PROVINCE $DISTRICT $TOWN"))
    }

    private companion object {
        const val PROVINCE = "서울"
        const val DISTRICT = "종로구"
        const val TOWN = "청운효자동"
    }
}
