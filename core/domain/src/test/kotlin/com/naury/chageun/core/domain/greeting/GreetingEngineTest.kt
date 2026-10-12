package com.naury.chageun.core.domain.greeting

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Test

class GreetingEngineTest {

    private val engine = GreetingEngine()

    // 2026-10-14은 수요일, 공휴일 아님
    private val wednesday = LocalDate.of(2026, 10, 14)
    private val freshMileage = MileageReading(wednesday, Kilometers(42_180))

    private fun facts(
        date: LocalDate = wednesday,
        hour: Int = 10,
        minute: Int = 0,
        mileage: MileageReading? = freshMileage.copy(date = date),
    ) = GreetingFacts(now = LocalDateTime.of(date, LocalTime.of(hour, minute)), mileage = mileage)

    private fun situationAt(hour: Int, date: LocalDate = wednesday) = engine.greet(facts(date, hour)).situation

    @Test
    fun timeOfDay_coversWholeDay() {
        assertThat(situationAt(2)).isEqualTo(GreetingSituation.Dawn)
        assertThat(situationAt(6)).isEqualTo(GreetingSituation.Morning)
        assertThat(situationAt(11)).isEqualTo(GreetingSituation.Lunch)
        assertThat(situationAt(20)).isEqualTo(GreetingSituation.Evening)
        assertThat(situationAt(23)).isEqualTo(GreetingSituation.Night)
    }

    @Test
    fun commuteAndWeekdays() {
        assertThat(situationAt(8)).isEqualTo(GreetingSituation.CommuteMorning)
        assertThat(situationAt(17)).isEqualTo(GreetingSituation.CommuteEvening)
        assertThat(situationAt(9, LocalDate.of(2026, 10, 12))).isEqualTo(GreetingSituation.MondayMorning)
        assertThat(situationAt(16, LocalDate.of(2026, 10, 16))).isEqualTo(GreetingSituation.FridayEvening)
        assertThat(situationAt(10, LocalDate.of(2026, 10, 17))).isEqualTo(GreetingSituation.Weekend)
        // 주말 출근 시간은 출근 인사가 아니다.
        assertThat(situationAt(8, LocalDate.of(2026, 10, 18))).isEqualTo(GreetingSituation.Weekend)
    }

    @Test
    fun seasonsShowInTheAfternoon() {
        assertThat(situationAt(14, LocalDate.of(2026, 4, 15))).isEqualTo(GreetingSituation.SpringAfternoon)
        assertThat(situationAt(14, LocalDate.of(2026, 7, 15))).isEqualTo(GreetingSituation.SummerAfternoon)
        assertThat(situationAt(14, wednesday)).isEqualTo(GreetingSituation.AutumnAfternoon)
        assertThat(situationAt(14, LocalDate.of(2026, 1, 14))).isEqualTo(GreetingSituation.WinterAfternoon)
    }

    @Test
    fun fixedHolidays_beatWeekdayRhythm() {
        val expected = mapOf(
            LocalDate.of(2027, 1, 1) to GreetingSituation.NewYear,
            LocalDate.of(2027, 3, 1) to GreetingSituation.IndependenceMovementDay,
            LocalDate.of(2026, 5, 5) to GreetingSituation.ChildrensDay,
            LocalDate.of(2026, 6, 6) to GreetingSituation.MemorialDay,
            LocalDate.of(2026, 8, 15) to GreetingSituation.LiberationDay,
            LocalDate.of(2026, 10, 3) to GreetingSituation.NationalFoundationDay,
            LocalDate.of(2026, 10, 9) to GreetingSituation.HangulDay,
            LocalDate.of(2026, 12, 25) to GreetingSituation.Christmas,
        )
        // 출근 시간이어도 공휴일 인사가 먼저다.
        expected.forEach { (date, situation) -> assertThat(situationAt(8, date)).isEqualTo(situation) }
        assertThat(KoreanHoliday.on(LocalDate.of(2026, 10, 10))).isNull()
    }

    @Test
    fun safetyBeatsEverything_inOrder() {
        val christmasMorning = facts(LocalDate.of(2026, 12, 25), 8)
        val all = christmasMorning.copy(
            hasActiveRecall = true,
            isInspectionOverdue = true,
            overdueItemCount = 2,
            isInspectionDueSoon = true,
            inspectionDaysLeft = 5,
            isFirstVisit = true,
        )

        assertThat(engine.greet(all).situation).isEqualTo(GreetingSituation.Recall)
        assertThat(engine.greet(all.copy(hasActiveRecall = false)).situation)
            .isEqualTo(GreetingSituation.InspectionOverdue)
        val overdue = engine.greet(all.copy(hasActiveRecall = false, isInspectionOverdue = false))
        assertThat(overdue.situation).isEqualTo(GreetingSituation.MaintenanceOverdue)
        assertThat(overdue.number).isEqualTo(2)
        val soon = engine.greet(all.copy(hasActiveRecall = false, isInspectionOverdue = false, overdueItemCount = 0))
        assertThat(soon.situation).isEqualTo(GreetingSituation.InspectionSoon)
        assertThat(soon.number).isEqualTo(5)
    }

    @Test
    fun firstVisit_andWelcomeBack() {
        assertThat(engine.greet(facts().copy(isFirstVisit = true)).situation).isEqualTo(GreetingSituation.FirstVisit)

        val back = engine.greet(facts().copy(previousVisitDate = wednesday.minusDays(20)))
        assertThat(back.situation).isEqualTo(GreetingSituation.WelcomeBack)
        assertThat(back.number).isEqualTo(20)

        val recent = engine.greet(facts().copy(previousVisitDate = wednesday.minusDays(3)))
        assertThat(recent.situation).isNotEqualTo(GreetingSituation.WelcomeBack)
    }

    @Test
    fun carMoments() {
        val anniversary = engine.greet(facts().copy(firstRegistrationDate = LocalDate.of(2023, 10, 14)))
        assertThat(anniversary.situation).isEqualTo(GreetingSituation.CarAnniversary)
        assertThat(anniversary.number).isEqualTo(3)
        // 등록한 그날은 기념일이 아니다.
        assertThat(engine.greet(facts().copy(firstRegistrationDate = wednesday)).situation)
            .isNotEqualTo(GreetingSituation.CarAnniversary)

        val milestone = engine.greet(facts(mileage = MileageReading(wednesday.minusDays(2), Kilometers(50_320))))
        assertThat(milestone.situation).isEqualTo(GreetingSituation.MileageMilestone)
        assertThat(milestone.number).isEqualTo(50_000)
        // 오래전에 적은 값이나 단위를 한참 넘은 값은 축하하지 않는다.
        assertThat(engine.greet(facts(mileage = MileageReading(wednesday.minusDays(20), Kilometers(50_320)))).situation)
            .isNotEqualTo(GreetingSituation.MileageMilestone)
        assertThat(engine.greet(facts(mileage = MileageReading(wednesday, Kilometers(56_000)))).situation)
            .isNotEqualTo(GreetingSituation.MileageMilestone)

        val recorded = engine.greet(facts(hour = 14).copy(lastRecordDate = wednesday.minusDays(1)))
        assertThat(recorded.situation).isEqualTo(GreetingSituation.RecordedRecently)
    }

    @Test
    fun staleMileage_onlyNudgesInTheEvening() {
        val stale = MileageReading(wednesday.minusDays(45), Kilometers(42_000))

        assertThat(engine.greet(facts(hour = 20, mileage = stale)).situation).isEqualTo(GreetingSituation.MileageStale)
        assertThat(engine.greet(facts(hour = 20, mileage = null)).situation).isEqualTo(GreetingSituation.MileageStale)
        assertThat(engine.greet(facts(hour = 14, mileage = stale)).situation)
            .isEqualTo(GreetingSituation.AutumnAfternoon)
    }

    @Test
    fun variant_isStableWithinADay_andRotatesDaily() {
        val morning = engine.greet(facts(hour = 6))
        val laterMorning = engine.greet(facts(hour = 6, minute = 59))
        val nextDay = engine.greet(facts(wednesday.plusDays(1), hour = 6))

        assertThat(morning.variantIndex(3)).isEqualTo(laterMorning.variantIndex(3))
        assertThat(nextDay.variantIndex(3)).isEqualTo((morning.variantIndex(3) + 1) % 3)
        assertThat(morning.variantIndex(0)).isEqualTo(0)
        // 1970년 이전 날짜에서도 음수가 나오지 않는다.
        assertThat(Greeting(GreetingSituation.Morning, seed = -7).variantIndex(3)).isEqualTo(2)
    }
}
