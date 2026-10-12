package com.naury.chageun.core.domain.greeting

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.util.concurrent.ConcurrentHashMap

/**
 * 관공서 공휴일과 그 대체공휴일. 같은 날 두 공휴일이 겹치면 위에 있는 것을 인사말에 쓴다
 * (예: 2028년 10월 3일은 개천절보다 추석이 먼저다).
 *
 * [substitution]은 주말과 겹칠 때 대체공휴일을 주는지다. 설·추석 연휴는 일요일과 겹칠 때만,
 * 3·1절·노동절·어린이날·부처님오신날·제헌절·광복절·개천절·한글날·성탄절은 토·일요일과 겹칠 때 준다.
 * 신정과 현충일은 주지 않는다. 다른 공휴일과 겹쳐도 대체공휴일을 준다.
 */
enum class KoreanHoliday(internal val substitution: Substitution) {
    Seollal(Substitution.OnSunday),
    SeollalEve(Substitution.OnSunday),
    SeollalAfter(Substitution.OnSunday),
    Chuseok(Substitution.OnSunday),
    ChuseokEve(Substitution.OnSunday),
    ChuseokAfter(Substitution.OnSunday),
    BuddhasBirthday(Substitution.OnWeekend),
    NewYear(Substitution.None),
    IndependenceMovementDay(Substitution.OnWeekend),
    LaborDay(Substitution.OnWeekend),
    ChildrensDay(Substitution.OnWeekend),
    MemorialDay(Substitution.None),
    ConstitutionDay(Substitution.OnWeekend),
    LiberationDay(Substitution.OnWeekend),
    NationalFoundationDay(Substitution.OnWeekend),
    HangulDay(Substitution.OnWeekend),
    Christmas(Substitution.OnWeekend),
    SubstituteHoliday(Substitution.None),
    ;

    internal enum class Substitution { None, OnSunday, OnWeekend }

    companion object {
        /** [date]의 공휴일. 여럿이 겹치면 위에 있는 것이고, 공휴일이 아니면 null이다. */
        fun on(date: LocalDate): KoreanHoliday? = KoreanHolidayCalendar.on(date).minOrNull()
    }
}

/**
 * 해마다 공휴일 달력을 만든다. 날짜가 고정된 공휴일은 어느 해든 넣고, 음력 명절은 [LUNAR] 표에 있는 해만 넣는다.
 * 대체공휴일은 「관공서의 공휴일에 관한 규정」 제3조의 규칙으로 계산한다.
 */
internal object KoreanHolidayCalendar {

    private val FIXED: Map<MonthDay, KoreanHoliday> = mapOf(
        "--01-01" to KoreanHoliday.NewYear,
        "--03-01" to KoreanHoliday.IndependenceMovementDay,
        // 노동절과 제헌절은 2026년부터 공휴일이다.
        "--05-01" to KoreanHoliday.LaborDay,
        "--05-05" to KoreanHoliday.ChildrensDay,
        "--06-06" to KoreanHoliday.MemorialDay,
        "--07-17" to KoreanHoliday.ConstitutionDay,
        "--08-15" to KoreanHoliday.LiberationDay,
        "--10-03" to KoreanHoliday.NationalFoundationDay,
        "--10-09" to KoreanHoliday.HangulDay,
        "--12-25" to KoreanHoliday.Christmas,
    ).mapKeys { MonthDay.parse(it.key) }

    /**
     * 설날·부처님오신날·추석 당일(양력). 2026·2027년은 월력요항에 실린 날이고, 2028~2030년은
     * 한국천문연구원 음양력 변환 값이다. 표에 없는 해는 음력 명절 없이 고정 공휴일만 쓴다.
     */
    private val LUNAR: Map<Int, LunarDates> = listOf(
        LunarDates("2026-02-17", "2026-05-24", "2026-09-25"),
        LunarDates("2027-02-07", "2027-05-13", "2027-09-15"),
        LunarDates("2028-01-27", "2028-05-02", "2028-10-03"),
        LunarDates("2029-02-13", "2029-05-20", "2029-09-22"),
        LunarDates("2030-02-03", "2030-05-09", "2030-09-12"),
    ).associateBy { it.seollal.year }

    /** 음력 명절 날짜를 아는 해. */
    val lunarYears: Set<Int> = LUNAR.keys

    private val byYear = ConcurrentHashMap<Int, Map<LocalDate, List<KoreanHoliday>>>()

    fun on(date: LocalDate): List<KoreanHoliday> = byYear.getOrPut(date.year) { holidaysOf(date.year) }[date].orEmpty()

    private fun holidaysOf(year: Int): Map<LocalDate, List<KoreanHoliday>> {
        val days = FIXED.entries.map { (monthDay, holiday) -> monthDay.atYear(year) to holiday }.toMutableList()
        LUNAR[year]?.let { lunar ->
            days += lunar.seollal.minusDays(1) to KoreanHoliday.SeollalEve
            days += lunar.seollal to KoreanHoliday.Seollal
            days += lunar.seollal.plusDays(1) to KoreanHoliday.SeollalAfter
            days += lunar.buddhasBirthday to KoreanHoliday.BuddhasBirthday
            days += lunar.chuseok.minusDays(1) to KoreanHoliday.ChuseokEve
            days += lunar.chuseok to KoreanHoliday.Chuseok
            days += lunar.chuseok.plusDays(1) to KoreanHoliday.ChuseokAfter
        }
        val holidays = days.groupBy({ it.first }, { it.second })
        return holidays + substitutesFor(holidays)
    }

    /**
     * 겹친 날마다 그다음 첫 평일(주말·공휴일이 아닌 날)을 대체공휴일로 둔다. 연휴 중에 겹치면
     * 연휴가 끝난 다음 평일이 된다. 앞 날짜부터 채우므로 대체공휴일끼리도 겹치지 않는다.
     */
    private fun substitutesFor(holidays: Map<LocalDate, List<KoreanHoliday>>): Map<LocalDate, List<KoreanHoliday>> {
        val taken = holidays.keys.toMutableSet()
        val substitutes = mutableMapOf<LocalDate, List<KoreanHoliday>>()
        holidays.keys.sorted().forEach { date ->
            repeat(lostDays(date, holidays.getValue(date))) {
                var day = date.plusDays(1)
                while (day.isWeekend() || day in taken) day = day.plusDays(1)
                taken += day
                substitutes[day] = listOf(KoreanHoliday.SubstituteHoliday)
            }
        }
        return substitutes
    }

    /** [date]에 쉬지 못하게 된 공휴일 수. 주말과 겹친 공휴일과, 평일에 겹친 두 번째 공휴일부터 센다. */
    private fun lostDays(date: LocalDate, holidays: List<KoreanHoliday>): Int {
        val eligible = holidays.filter { it.substitution != KoreanHoliday.Substitution.None }
        val onWeekend = eligible.count { it.substitution.covers(date.dayOfWeek) }
        return if (onWeekend > 0) onWeekend else minOf(eligible.size, holidays.size - 1)
    }

    private fun KoreanHoliday.Substitution.covers(day: DayOfWeek): Boolean = when (this) {
        KoreanHoliday.Substitution.None -> false
        KoreanHoliday.Substitution.OnSunday -> day == DayOfWeek.SUNDAY
        KoreanHoliday.Substitution.OnWeekend -> day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
    }

    private fun LocalDate.isWeekend(): Boolean = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

    private class LunarDates(seollal: String, buddhasBirthday: String, chuseok: String) {
        val seollal: LocalDate = LocalDate.parse(seollal)
        val buddhasBirthday: LocalDate = LocalDate.parse(buddhasBirthday)
        val chuseok: LocalDate = LocalDate.parse(chuseok)
    }
}
