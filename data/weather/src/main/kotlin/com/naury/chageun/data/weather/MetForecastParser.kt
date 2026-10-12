package com.naury.chageun.data.weather

import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherSnapshot
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * MET Norway Locationforecast 2.0 compact 응답에서 지금 시각의 날씨를 꺼낸다.
 * 문서: https://api.met.no/weatherapi/locationforecast/2.0/documentation
 */
internal object MetForecastParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** 읽을 수 없는 응답이면 null이다. */
    fun parse(body: String, now: Instant): WeatherSnapshot? {
        val forecast = decodeOrNull(body) ?: return null
        val steps = forecast.properties.timeseries.mapNotNull { step -> step.instantOrNull()?.let { it to step } }
        // 지금을 지나지 않은 가장 늦은 시각을 쓴다. 응답이 모두 미래면 첫 칸을 쓴다.
        val (time, step) = steps.lastOrNull { (time, _) -> !time.isAfter(now) } ?: steps.firstOrNull() ?: return null
        return step.toSnapshot(time)
    }

    private fun decodeOrNull(body: String): MetForecast? = try {
        json.decodeFromString<MetForecast>(body)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun MetTimeStep.toSnapshot(time: Instant): WeatherSnapshot? {
        val symbol = (data.next1Hours ?: data.next6Hours)?.summary?.symbolCode
        val temperature = data.instant.details.airTemperature
        if (symbol == null || temperature == null) return null
        return WeatherSnapshot(
            condition = MetSymbols.condition(symbol),
            temperatureC = temperature,
            windSpeedMs = data.instant.details.windSpeed ?: 0.0,
            isNight = MetSymbols.isNight(symbol),
            observedAt = time,
        )
    }

    private fun MetTimeStep.instantOrNull(): Instant? = try {
        Instant.parse(time)
    } catch (_: DateTimeParseException) {
        null
    }
}

/** MET의 symbol_code(예: "lightrainshowers_day")를 앱의 [WeatherCondition]으로 묶는다. */
internal object MetSymbols {
    // 위에서부터 먼저 맞는 것을 쓴다. "sleetshowersandthunder"는 뇌우로, "heavysnow"는 눈으로 본다.
    private val rules: List<Pair<(String) -> Boolean, WeatherCondition>> = listOf(
        { code: String -> "thunder" in code } to WeatherCondition.Thunder,
        { code: String -> "sleet" in code } to WeatherCondition.Sleet,
        { code: String -> "snow" in code } to WeatherCondition.Snow,
        { code: String -> code.startsWith("heavyrain") } to WeatherCondition.HeavyRain,
        { code: String -> "rain" in code } to WeatherCondition.Rain,
        { code: String -> code == "fog" } to WeatherCondition.Fog,
        { code: String -> code == "clearsky" || code == "fair" } to WeatherCondition.Clear,
        { code: String -> code == "partlycloudy" } to WeatherCondition.PartlyCloudy,
    )

    fun condition(symbolCode: String): WeatherCondition {
        val base = symbolCode.substringBefore('_')
        return rules.firstOrNull { (matches, _) -> matches(base) }?.second ?: WeatherCondition.Cloudy
    }

    fun isNight(symbolCode: String): Boolean = symbolCode.endsWith("_night")
}

@Serializable
internal data class MetForecast(val properties: MetProperties)

@Serializable
internal data class MetProperties(val timeseries: List<MetTimeStep> = emptyList())

@Serializable
internal data class MetTimeStep(val time: String, val data: MetStepData)

@Serializable
internal data class MetStepData(
    val instant: MetInstant,
    @SerialName("next_1_hours") val next1Hours: MetPeriod? = null,
    @SerialName("next_6_hours") val next6Hours: MetPeriod? = null,
)

@Serializable
internal data class MetInstant(val details: MetInstantDetails)

@Serializable
internal data class MetInstantDetails(
    @SerialName("air_temperature") val airTemperature: Double? = null,
    @SerialName("wind_speed") val windSpeed: Double? = null,
)

@Serializable
internal data class MetPeriod(val summary: MetSummary? = null)

@Serializable
internal data class MetSummary(@SerialName("symbol_code") val symbolCode: String)
