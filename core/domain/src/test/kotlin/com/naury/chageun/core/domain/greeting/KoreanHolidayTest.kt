package com.naury.chageun.core.domain.greeting

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Test

class KoreanHolidayTest {

    private fun days(year: Int) =
        generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }.takeWhile { it.year == year }

    private fun substitutes(year: Int): List<String> =
        days(year).filter { KoreanHoliday.on(it) == KoreanHoliday.SubstituteHoliday }.map { it.toString() }.toList()

    @Test
    fun lunarHolidays_matchOfficialCalendar() {
        val expected = mapOf(
            "2026-02-17" to KoreanHoliday.Seollal,
            "2026-05-24" to KoreanHoliday.BuddhasBirthday,
            "2026-09-25" to KoreanHoliday.Chuseok,
            "2027-02-07" to KoreanHoliday.Seollal,
            "2027-05-13" to KoreanHoliday.BuddhasBirthday,
            "2027-09-15" to KoreanHoliday.Chuseok,
            "2028-01-27" to KoreanHoliday.Seollal,
            "2028-05-02" to KoreanHoliday.BuddhasBirthday,
            // 개천절과 겹치면 추석을 먼저 인사한다.
            "2028-10-03" to KoreanHoliday.Chuseok,
            "2029-02-13" to KoreanHoliday.Seollal,
            "2029-05-20" to KoreanHoliday.BuddhasBirthday,
            "2029-09-22" to KoreanHoliday.Chuseok,
            "2030-02-03" to KoreanHoliday.Seollal,
            "2030-05-09" to KoreanHoliday.BuddhasBirthday,
            "2030-09-12" to KoreanHoliday.Chuseok,
        )
        expected.forEach { (date, holiday) ->
            assertWithMessage(date).that(KoreanHoliday.on(LocalDate.parse(date))).isEqualTo(holiday)
        }
        assertThat(KoreanHoliday.on(LocalDate.of(2026, 2, 16))).isEqualTo(KoreanHoliday.SeollalEve)
        assertThat(KoreanHoliday.on(LocalDate.of(2026, 2, 18))).isEqualTo(KoreanHoliday.SeollalAfter)
        assertThat(KoreanHoliday.on(LocalDate.of(2026, 9, 24))).isEqualTo(KoreanHoliday.ChuseokEve)
        assertThat(KoreanHoliday.on(LocalDate.of(2026, 9, 26))).isEqualTo(KoreanHoliday.ChuseokAfter)
        assertThat(KoreanHolidayCalendar.lunarYears).containsExactly(2026, 2027, 2028, 2029, 2030)
    }

    /** 월력요항에 실린 대체공휴일과 같다. 2028~2030년은 같은 규칙으로 계산한 값이다. */
    @Test
    fun substituteHolidays_followTheRules() {
        assertThat(substitutes(2026)).containsExactly("2026-03-02", "2026-05-25", "2026-08-17", "2026-10-05")
            .inOrder()
        assertThat(substitutes(2027)).containsExactly(
            "2027-02-09",
            "2027-05-03",
            "2027-07-19",
            "2027-08-16",
            "2027-10-04",
            "2027-10-11",
            "2027-12-27",
        ).inOrder()
        assertThat(substitutes(2028)).containsExactly("2028-10-05")
        assertThat(substitutes(2029)).containsExactly("2029-05-07", "2029-05-21", "2029-09-24").inOrder()
        assertThat(substitutes(2030)).containsExactly("2030-02-05", "2030-05-06").inOrder()
    }

    @Test
    fun yearsWithoutLunarTable_keepFixedHolidaysAndTheirSubstitutes() {
        assertThat(KoreanHoliday.on(LocalDate.of(2031, 1, 1))).isEqualTo(KoreanHoliday.NewYear)
        // 2031년 3월 1일은 토요일이라 3일 월요일이 대체공휴일이다.
        assertThat(KoreanHoliday.on(LocalDate.of(2031, 3, 3))).isEqualTo(KoreanHoliday.SubstituteHoliday)
        // 신정·현충일은 주말과 겹쳐도 대체공휴일이 없다. 2028년 1월 1일은 토요일이다.
        assertThat(KoreanHoliday.on(LocalDate.of(2028, 1, 3))).isNull()
    }

    @Test
    fun greetings_forLunarHolidays_andTravelDays() {
        val engine = GreetingEngine()
        fun at(date: String, hour: Int = 8) =
            engine.greet(GreetingFacts(now = LocalDateTime.of(LocalDate.parse(date), LocalTime.of(hour, 0)))).situation

        assertThat(at("2026-02-17")).isEqualTo(GreetingSituation.Seollal)
        assertThat(at("2026-02-16")).isEqualTo(GreetingSituation.SeollalTravel)
        assertThat(at("2026-02-18")).isEqualTo(GreetingSituation.SeollalTravel)
        assertThat(at("2026-09-25")).isEqualTo(GreetingSituation.Chuseok)
        assertThat(at("2026-09-24")).isEqualTo(GreetingSituation.ChuseokTravel)
        assertThat(at("2026-05-24")).isEqualTo(GreetingSituation.BuddhasBirthday)
        assertThat(at("2026-05-25")).isEqualTo(GreetingSituation.SubstituteHoliday)
        assertThat(at("2026-05-01")).isEqualTo(GreetingSituation.LaborDay)
        assertThat(at("2026-07-17")).isEqualTo(GreetingSituation.ConstitutionDay)
        // 대체공휴일 다음 날 아침은 다시 출근 인사다.
        assertThat(at("2026-05-26")).isEqualTo(GreetingSituation.CommuteMorning)
    }

    @Test
    fun holidayGreetings_stayBelowSafetyAndWelcome() {
        val engine = GreetingEngine()
        val seollal = GreetingFacts(now = LocalDateTime.of(2027, 2, 7, 10, 0))

        assertThat(engine.greet(seollal.copy(overdueItemCount = 1)).situation)
            .isEqualTo(GreetingSituation.MaintenanceOverdue)
        assertThat(engine.greet(seollal.copy(isFirstVisit = true)).situation).isEqualTo(GreetingSituation.FirstVisit)
        assertThat(engine.greet(seollal.copy(firstRegistrationDate = LocalDate.of(2024, 2, 7))).situation)
            .isEqualTo(GreetingSituation.Seollal)
    }
}
