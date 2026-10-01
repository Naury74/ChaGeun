package com.naury.chageun.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlateNumberTest {

    private fun valid(input: String) = (PlateNumber.parse(input) as PlateParseResult.Valid).plate

    @Test
    fun parsesCurrentThreeDigitFormat() {
        val plate = valid("123가4567")

        assertThat(plate.classNumber).isEqualTo("123")
        assertThat(plate.usage).isEqualTo('가')
        assertThat(plate.serial).isEqualTo("4567")
        assertThat(plate.region).isNull()
    }

    @Test
    fun parsesLegacyTwoDigitAndRegionalFormats() {
        assertThat(valid("12가3456").normalized).isEqualTo("12가3456")
        assertThat(valid("서울12가3456").region).isEqualTo("서울")
    }

    @Test
    fun normalizesSpacesAndHyphens() {
        assertThat(valid(" 123 가 4567 ").normalized).isEqualTo("123가4567")
        assertThat(valid("123가-4567").normalized).isEqualTo("123가4567")
    }

    @Test
    fun acceptsCommercialAndRentalLetters() {
        assertThat(PlateNumber.parse("12바3456")).isInstanceOf(PlateParseResult.Valid::class.java)
        assertThat(PlateNumber.parse("12하3456")).isInstanceOf(PlateParseResult.Valid::class.java)
    }

    @Test
    fun reportsComposing_forStandaloneJamo() {
        assertThat(PlateNumber.parse("123ㄱ")).isEqualTo(PlateParseResult.Composing)
        assertThat(PlateNumber.parse("123가45ㅏ")).isEqualTo(PlateParseResult.Composing)
    }

    @Test
    fun rejectsMalformedInput() {
        assertThat(PlateNumber.parse("")).isEqualTo(PlateParseResult.Empty)
        assertThat(PlateNumber.parse("1가4567")).isEqualTo(PlateParseResult.InvalidFormat)
        assertThat(PlateNumber.parse("1234가4567")).isEqualTo(PlateParseResult.InvalidFormat)
        assertThat(PlateNumber.parse("123가456")).isEqualTo(PlateParseResult.InvalidFormat)
        assertThat(PlateNumber.parse("평양12가3456")).isEqualTo(PlateParseResult.InvalidFormat)
        assertThat(PlateNumber.parse("ABC1234")).isEqualTo(PlateParseResult.InvalidFormat)
    }

    @Test
    fun rejectsOutOfScopeUsageLetters() {
        assertThat(PlateNumber.parse("12육3456")).isEqualTo(PlateParseResult.UnsupportedUsage)
        assertThat(PlateNumber.parse("12외3456")).isEqualTo(PlateParseResult.UnsupportedUsage)
    }

    @Test
    fun masksSerialInDisplayAndToString() {
        val plate = valid("123가4567")

        assertThat(plate.masked).isEqualTo("123가 **67")
        assertThat("$plate").doesNotContain("4567")
    }

    @Test
    fun comparesByNormalizedValue() {
        assertThat(valid("123 가 4567")).isEqualTo(valid("123가4567"))
    }
}
