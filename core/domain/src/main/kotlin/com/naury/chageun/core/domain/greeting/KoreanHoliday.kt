package com.naury.chageun.core.domain.greeting

import java.time.LocalDate
import java.time.MonthDay

/**
 * 날짜가 해마다 같은 공휴일과 기념일. 설·추석·부처님오신날처럼 음력으로 정하는 날은
 * 해마다 달력이 필요해 넣지 않았다.
 */
enum class KoreanHoliday(isoMonthDay: String) {
    NewYear("--01-01"),
    IndependenceMovementDay("--03-01"),
    ChildrensDay("--05-05"),
    MemorialDay("--06-06"),
    LiberationDay("--08-15"),
    NationalFoundationDay("--10-03"),
    HangulDay("--10-09"),
    Christmas("--12-25"),
    ;

    val monthDay: MonthDay = MonthDay.parse(isoMonthDay)

    companion object {
        fun on(date: LocalDate): KoreanHoliday? = entries.firstOrNull { it.monthDay == MonthDay.from(date) }
    }
}
