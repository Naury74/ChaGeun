package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherSnapshot

/** 날씨에 맞춘 운전 팁. [isCaution]이면 홈에서 주의 색으로 보여 준다. */
enum class WeatherTip(val isCaution: Boolean) {
    Snow(isCaution = true),
    Thunder(isCaution = true),
    HeavyRain(isCaution = true),
    Rain(isCaution = true),
    Fog(isCaution = true),
    StrongWind(isCaution = true),
    Cold(isCaution = true),
    Freezing(isCaution = true),
    Heat(isCaution = true),
    NiceDay(isCaution = false),
    Calm(isCaution = false),
}

/**
 * 날씨 팁 규칙. 위에서부터 먼저 맞는 하나를 고른다. 노면이 미끄럽거나 앞이 안 보이는 날을
 * 기온·바람보다 앞에 두고, 맑은 날 세차·드라이브 권유는 가장 뒤에 둔다.
 */
object WeatherTips {
    // 기상청 강풍주의보 기준(초속 14m)과 폭염경보 기준(33도)을 따른다.
    const val STRONG_WIND_MS = 14.0
    const val HEAT_C = 33.0
    const val COLD_C = -5.0
    const val FREEZING_C = 0.0
    private const val NICE_MIN_C = 10.0
    private const val NICE_MAX_C = 28.0

    private val rules: List<Pair<WeatherTip, (WeatherSnapshot) -> Boolean>> = listOf(
        WeatherTip.Snow to { it.condition == WeatherCondition.Snow || it.condition == WeatherCondition.Sleet },
        WeatherTip.Thunder to { it.condition == WeatherCondition.Thunder },
        WeatherTip.HeavyRain to { it.condition == WeatherCondition.HeavyRain },
        WeatherTip.Rain to { it.condition == WeatherCondition.Rain },
        WeatherTip.Fog to { it.condition == WeatherCondition.Fog },
        WeatherTip.StrongWind to { it.windSpeedMs >= STRONG_WIND_MS },
        WeatherTip.Cold to { it.temperatureC <= COLD_C },
        WeatherTip.Freezing to { it.temperatureC <= FREEZING_C },
        WeatherTip.Heat to { it.temperatureC >= HEAT_C },
        WeatherTip.NiceDay to { it.isNiceForDriving() },
    )

    fun tipFor(snapshot: WeatherSnapshot): WeatherTip =
        rules.firstOrNull { (_, matches) -> matches(snapshot) }?.first ?: WeatherTip.Calm

    private fun WeatherSnapshot.isNiceForDriving(): Boolean = !isNight &&
        (condition == WeatherCondition.Clear || condition == WeatherCondition.PartlyCloudy) &&
        temperatureC in NICE_MIN_C..NICE_MAX_C
}
