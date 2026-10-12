package com.naury.chageun.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.naury.chageun.core.domain.weather.Daylight
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherSnapshot
import java.time.Instant
import org.junit.Test

class WeatherSkyTest {

    private fun snapshot(
        condition: WeatherCondition,
        temperatureC: Double = 18.0,
        windSpeedMs: Double = 2.0,
        isNight: Boolean = false,
    ) = WeatherSnapshot(condition, temperatureC, windSpeedMs, isNight, Instant.EPOCH)

    @Test
    fun scenes_followWeatherAndTime() {
        val cases = mapOf(
            snapshot(WeatherCondition.Clear) to WeatherScene.ClearDay,
            snapshot(WeatherCondition.Clear, isNight = true) to WeatherScene.ClearNight,
            snapshot(WeatherCondition.PartlyCloudy) to WeatherScene.PartlyCloudy,
            snapshot(WeatherCondition.Cloudy) to WeatherScene.Cloudy,
            snapshot(WeatherCondition.Cloudy, windSpeedMs = 16.0) to WeatherScene.Wind,
            snapshot(WeatherCondition.Fog) to WeatherScene.Fog,
            snapshot(WeatherCondition.Rain) to WeatherScene.Rain,
            snapshot(WeatherCondition.HeavyRain) to WeatherScene.HeavyRain,
            snapshot(WeatherCondition.Thunder) to WeatherScene.Thunder,
            snapshot(WeatherCondition.Sleet) to WeatherScene.Snow,
            snapshot(WeatherCondition.Clear, temperatureC = 35.0) to WeatherScene.Heat,
            snapshot(WeatherCondition.Clear, temperatureC = -8.0) to WeatherScene.Cold,
        )

        cases.forEach { (weather, scene) -> assertThat(weatherSceneFor(weather, Daylight.Day)).isEqualTo(scene) }
        // 해가 진 뒤 맑은 하늘은 제공처가 낮 아이콘을 줘도 밤이다.
        assertThat(weatherSceneFor(snapshot(WeatherCondition.Clear), Daylight.Night)).isEqualTo(WeatherScene.ClearNight)
        // 해 지기 전 30분은 노을이다. 구름 조금도 노을로 그리지만, 비·눈 같은 날씨는 그대로다.
        assertThat(weatherSceneFor(snapshot(WeatherCondition.Clear), Daylight.Dusk)).isEqualTo(WeatherScene.Dusk)
        assertThat(weatherSceneFor(snapshot(WeatherCondition.PartlyCloudy), Daylight.Dusk))
            .isEqualTo(WeatherScene.Dusk)
        assertThat(weatherSceneFor(snapshot(WeatherCondition.Rain), Daylight.Dusk)).isEqualTo(WeatherScene.Rain)
        // 제공처가 밤 아이콘을 줬으면 노을 시간이어도 밤이다.
        assertThat(weatherSceneFor(snapshot(WeatherCondition.Clear, isNight = true), Daylight.Dusk))
            .isEqualTo(WeatherScene.ClearNight)
    }

    /** 하늘 위 글자(브랜드·인사말·날짜·날씨 줄·차 이름·연식)가 어떤 날씨에서도 4.5:1 이상이다. */
    @Test
    fun textOnEverySky_keepsContrast() {
        val cases = listOf(false, true).flatMap { isDark -> WeatherScene.entries.map { it to isDark } }
        cases.forEach { (scene, isDark) ->
            val sky = skyPalette(scene, isDark).sky
            val background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
            // 맨 위 하늘, 햇빛 번짐이 가장 진한 오른쪽 위, 그리고 차 이름 아래 연식이 놓이는 하늘 중간쯤.
            val behind = listOf(sky.top, sky.glow.compositeOver(sky.top), lerp(sky.top, background, HALF_WAY))
            val text = if (isDark) DARK_TEXT else LIGHT_TEXT
            text.flatMap { color -> behind.map { color to it } }.forEach { (color, back) ->
                assertWithMessage("$scene dark=$isDark $color on $back")
                    .that(contrast(color, back)).isAtLeast(MIN_CONTRAST)
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private companion object {
        const val MIN_CONTRAST = 4.5
        const val HALF_WAY = 0.5f

        // 글자색, 하늘 위 보조 글자색, 브랜드 글자색(대표 색)
        val LIGHT_TEXT = listOf(Color(0xFF1C1C1E), SKY_VARIANT_TEXT_LIGHT, Color(0xFF24345A))
        val DARK_TEXT = listOf(Color(0xFFF2F2F7), SKY_VARIANT_TEXT_DARK, Color(0xFFA8BBE8))
    }
}
