package com.naury.chageun.data.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.WeatherCondition
import java.time.Instant
import org.junit.Test

class MetForecastParserTest {

    private val body = checkNotNull(javaClass.classLoader?.getResource("met_compact_seoul.json")).readText()

    @Test
    fun picksStepForNow_withOneHourSymbol() {
        val snapshot = MetForecastParser.parse(body, Instant.parse("2026-10-12T05:40:00Z"))

        assertThat(snapshot?.condition).isEqualTo(WeatherCondition.Rain)
        assertThat(snapshot?.temperatureC).isEqualTo(17.9)
        assertThat(snapshot?.windSpeedMs).isEqualTo(3.4)
        assertThat(snapshot?.isNight).isFalse()
        assertThat(snapshot?.observedAt).isEqualTo(Instant.parse("2026-10-12T05:00:00Z"))
    }

    @Test
    fun movesToLaterStep_andFallsBackToSixHourSymbol() {
        val heavy = MetForecastParser.parse(body, Instant.parse("2026-10-12T06:10:00Z"))
        val later = MetForecastParser.parse(body, Instant.parse("2026-10-12T13:00:00Z"))

        assertThat(heavy?.condition).isEqualTo(WeatherCondition.HeavyRain)
        assertThat(later?.condition).isEqualTo(WeatherCondition.PartlyCloudy)
        assertThat(later?.isNight).isTrue()
    }

    @Test
    fun beforeFirstStep_usesFirstStep() {
        val snapshot = MetForecastParser.parse(body, Instant.parse("2026-10-12T01:00:00Z"))

        assertThat(snapshot?.temperatureC).isEqualTo(17.9)
    }

    @Test
    fun brokenOrEmptyBody_isNull() {
        assertThat(MetForecastParser.parse("{", Instant.EPOCH)).isNull()
        assertThat(MetForecastParser.parse("""{"properties":{"timeseries":[]}}""", Instant.EPOCH)).isNull()
    }

    @Test
    fun symbolCodes_mapToConditions() {
        val expected = mapOf(
            "clearsky_day" to WeatherCondition.Clear,
            "fair_night" to WeatherCondition.Clear,
            "partlycloudy_polartwilight" to WeatherCondition.PartlyCloudy,
            "cloudy" to WeatherCondition.Cloudy,
            "fog" to WeatherCondition.Fog,
            "lightrain" to WeatherCondition.Rain,
            "rainshowers_day" to WeatherCondition.Rain,
            "heavyrainshowers_night" to WeatherCondition.HeavyRain,
            "heavyrainandthunder" to WeatherCondition.Thunder,
            "lightsleetshowers_day" to WeatherCondition.Sleet,
            "heavysnow" to WeatherCondition.Snow,
            "snowshowersandthunder_day" to WeatherCondition.Thunder,
            "something_new" to WeatherCondition.Cloudy,
        )

        expected.forEach { (code, condition) -> assertThat(MetSymbols.condition(code)).isEqualTo(condition) }
    }
}
