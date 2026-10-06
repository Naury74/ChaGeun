package com.naury.chageun.core.domain.mileage

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate
import org.junit.Test

class OdometerParserTest {

    private val today = LocalDate.of(2026, 10, 6)
    private val previous = MileageReading(LocalDate.of(2026, 9, 6), Kilometers(45_000))

    @Test
    fun picksOdometer_andIgnoresClockTemperatureTripAndRange() {
        val lines = listOf(
            "12:34",
            "23°C",
            "TRIP A 123.4 km",
            "DTE 420 km",
            "ODO 45,830 km",
            "16.2 km/L",
        )

        val result = OdometerParser.parse(lines, previous, today)

        assertThat(result.best).isEqualTo(Kilometers(45_830))
        assertThat(result.others).isEmpty()
        assertThat(result.range).isEqualTo(Kilometers(420))
    }

    @Test
    fun rejectsValuesBelowLastReading_orTooFarForElapsedDays() {
        val result = OdometerParser.parse(listOf("44 900", "45 300", "145 300"), previous, today)

        assertThat(result.best).isEqualTo(Kilometers(45_300))
        assertThat(result.others).isEmpty()
    }

    @Test
    fun prefersOdoKeyword_thenCloserToLastReading() {
        val result = OdometerParser.parse(listOf("46200", "총주행 45900 km", "45100 km"), previous, today)

        assertThat(result.best).isEqualTo(Kilometers(45_900))
        assertThat(result.others).containsExactly(Kilometers(45_100), Kilometers(46_200)).inOrder()
    }

    @Test
    fun withoutHistory_acceptsAnyRealisticOdometer() {
        val result = OdometerParser.parse(listOf("주행가능거리 380km", "38,512 km", "5"), previous = null, today = today)

        assertThat(result.best).isEqualTo(Kilometers(38_512))
        assertThat(result.range).isEqualTo(Kilometers(380))
    }

    @Test
    fun returnsNothing_whenNoNumberFits() {
        val result = OdometerParser.parse(listOf("READY", "P R N D"), previous, today)

        assertThat(result.best).isNull()
        assertThat(result.others).isEmpty()
    }
}
