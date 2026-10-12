package com.naury.chageun.core.domain.greeting

import com.naury.chageun.core.domain.maintenance.MILEAGE_PROMPT_AFTER_DAYS
import java.time.DayOfWeek
import java.time.MonthDay
import java.time.temporal.ChronoUnit

/**
 * 인사말 규칙 목록. 순서가 곧 우선순위다.
 *
 * 1. 리콜·검사 기한·밀린 정비처럼 안전과 관련된 차 상태
 * 2. 처음 온 날, 오랜만에 온 날
 * 3. 공휴일·기념일. 설·추석 전날과 다음날은 귀성길·귀경길 안전 운전 인사다.
 * 4. 차를 등록한 날, 주행거리 돌파, 방금 남긴 기록
 * 5. 요일과 출퇴근 시간, 저녁의 주행거리 확인 권유
 * 6. 계절 오후
 * 7. 시간대(새벽·아침·점심·오후·저녁·밤). 늘 맞으므로 마지막이다.
 */
object GreetingRules {
    const val WELCOME_BACK_AFTER_DAYS = 14L
    const val MILESTONE_STEP_KM = 10_000L

    // 돌파한 지 얼마 안 된 값만 축하한다. 1만 km 단위에서 1,000km 안이고 일주일 안에 적은 주행거리여야 한다.
    private const val MILESTONE_WINDOW_KM = 1_000L
    private const val MILESTONE_RECENT_DAYS = 7L
    private const val RECENT_RECORD_DAYS = 1L

    private val safety = listOf(
        GreetingRule(GreetingSituation.Recall, { it.hasActiveRecall }),
        GreetingRule(GreetingSituation.InspectionOverdue, { it.isInspectionOverdue }),
        GreetingRule(
            GreetingSituation.MaintenanceOverdue,
            { it.overdueItemCount > 0 },
            { it.overdueItemCount.toLong() },
        ),
        GreetingRule(GreetingSituation.InspectionSoon, { it.isInspectionDueSoon }, { it.inspectionDaysLeft }),
    )

    private val welcome = listOf(
        GreetingRule(GreetingSituation.FirstVisit, { it.isFirstVisit }),
        GreetingRule(
            GreetingSituation.WelcomeBack,
            { (it.daysSinceVisit() ?: 0) >= WELCOME_BACK_AFTER_DAYS },
            { it.daysSinceVisit() },
        ),
    )

    private val holidays = KoreanHoliday.entries.map { it.situation }.distinct().map { situation ->
        GreetingRule(situation, { KoreanHoliday.on(it.today)?.situation == situation })
    }

    private val carMoments = listOf(
        GreetingRule(GreetingSituation.CarAnniversary, { it.carYears() != null }, { it.carYears() }),
        GreetingRule(GreetingSituation.MileageMilestone, { it.milestoneKm() != null }, { it.milestoneKm() }),
        GreetingRule(GreetingSituation.RecordedRecently, { facts ->
            facts.lastRecordDate?.let { ChronoUnit.DAYS.between(it, facts.today) in 0..RECENT_RECORD_DAYS } == true
        }),
    )

    private val weekRhythm = listOf(
        GreetingRule(GreetingSituation.MondayMorning, { it.isWorkday() && it.isMonday() && it.hour in 5 until 12 }),
        GreetingRule(GreetingSituation.FridayEvening, { it.isWorkday() && it.isFriday() && it.hour >= 15 }),
        GreetingRule(GreetingSituation.CommuteMorning, { it.isWorkday() && it.hour in 7 until 9 }),
        GreetingRule(GreetingSituation.CommuteEvening, { it.isWorkday() && it.hour in 17 until 20 }),
        GreetingRule(GreetingSituation.Weekend, { !it.isWeekday() && it.hour in 8 until 20 }),
        GreetingRule(GreetingSituation.MileageStale, { it.hour in 18 until 22 && it.isMileageStale() }),
    )

    private val seasons = listOf(
        GreetingSituation.SpringAfternoon to 3..5,
        GreetingSituation.SummerAfternoon to 6..8,
        GreetingSituation.AutumnAfternoon to 9..11,
    ).map { (situation, months) ->
        GreetingRule(situation, { it.hour in 13 until 18 && it.now.monthValue in months })
    } + GreetingRule(GreetingSituation.WinterAfternoon, { it.hour in 13 until 18 })

    /** 시간대 규칙 어디에도 안 맞을 때 쓰는 마지막 규칙. 시간대 규칙이 하루를 다 덮으므로 실제로는 쓰이지 않는다. */
    val timeOfDayFallback = GreetingRule(GreetingSituation.Afternoon, { true })

    private val timesOfDay = listOf(
        GreetingSituation.Dawn to (0 until 5),
        GreetingSituation.Morning to (5 until 11),
        GreetingSituation.Lunch to (11 until 13),
        GreetingSituation.Afternoon to (13 until 18),
        GreetingSituation.Evening to (18 until 22),
        GreetingSituation.Night to (22 until 24),
    ).map { (situation, hours) -> GreetingRule(situation, { it.hour in hours }) }

    val all: List<GreetingRule> = safety + welcome + holidays + carMoments + weekRhythm + seasons + timesOfDay

    private val KoreanHoliday.situation: GreetingSituation
        get() = when (this) {
            KoreanHoliday.Seollal -> GreetingSituation.Seollal
            KoreanHoliday.SeollalEve, KoreanHoliday.SeollalAfter -> GreetingSituation.SeollalTravel
            KoreanHoliday.Chuseok -> GreetingSituation.Chuseok
            KoreanHoliday.ChuseokEve, KoreanHoliday.ChuseokAfter -> GreetingSituation.ChuseokTravel
            KoreanHoliday.BuddhasBirthday -> GreetingSituation.BuddhasBirthday
            KoreanHoliday.NewYear -> GreetingSituation.NewYear
            KoreanHoliday.IndependenceMovementDay -> GreetingSituation.IndependenceMovementDay
            KoreanHoliday.LaborDay -> GreetingSituation.LaborDay
            KoreanHoliday.ChildrensDay -> GreetingSituation.ChildrensDay
            KoreanHoliday.MemorialDay -> GreetingSituation.MemorialDay
            KoreanHoliday.ConstitutionDay -> GreetingSituation.ConstitutionDay
            KoreanHoliday.LiberationDay -> GreetingSituation.LiberationDay
            KoreanHoliday.NationalFoundationDay -> GreetingSituation.NationalFoundationDay
            KoreanHoliday.HangulDay -> GreetingSituation.HangulDay
            KoreanHoliday.Christmas -> GreetingSituation.Christmas
            KoreanHoliday.SubstituteHoliday -> GreetingSituation.SubstituteHoliday
        }

    private fun GreetingFacts.isWeekday(): Boolean =
        now.dayOfWeek != DayOfWeek.SATURDAY && now.dayOfWeek != DayOfWeek.SUNDAY

    private fun GreetingFacts.isWorkday(): Boolean = isWeekday() && KoreanHoliday.on(today) == null

    private fun GreetingFacts.isMonday(): Boolean = now.dayOfWeek == DayOfWeek.MONDAY

    private fun GreetingFacts.isFriday(): Boolean = now.dayOfWeek == DayOfWeek.FRIDAY

    private fun GreetingFacts.daysSinceVisit(): Long? = previousVisitDate?.let { ChronoUnit.DAYS.between(it, today) }

    private fun GreetingFacts.isMileageStale(): Boolean =
        mileage?.let { ChronoUnit.DAYS.between(it.date, today) >= MILEAGE_PROMPT_AFTER_DAYS } ?: true

    /** 오늘이 처음 등록한 날과 같은 날짜면 함께한 햇수. 등록한 그해에는 null이다. */
    private fun GreetingFacts.carYears(): Long? {
        val registered = firstRegistrationDate ?: return null
        if (MonthDay.from(registered) != MonthDay.from(today)) return null
        return ChronoUnit.YEARS.between(registered, today).takeIf { it >= 1 }
    }

    /** 최근에 적은 주행거리가 1만 km 단위를 막 넘었으면 그 단위 값. */
    private fun GreetingFacts.milestoneKm(): Long? {
        val reading = mileage ?: return null
        if (ChronoUnit.DAYS.between(reading.date, today) !in 0..MILESTONE_RECENT_DAYS) return null
        val km = reading.mileage.value
        if (km < MILESTONE_STEP_KM || km % MILESTONE_STEP_KM >= MILESTONE_WINDOW_KM) return null
        return km - km % MILESTONE_STEP_KM
    }
}
