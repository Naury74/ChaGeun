package com.naury.chageun.core.domain.greeting

import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 인사말을 고르는 데 쓰는 사실들. 화면이나 시계에 기대지 않아 테스트에서 그대로 만들 수 있다.
 *
 * @param previousVisitDate 이번 실행 전에 마지막으로 앱을 연 날. 모르면 null이다.
 * @param inspectionDaysLeft 검사 기한까지 남은 날. 기한이 지났으면 음수다.
 */
data class GreetingFacts(
    val now: LocalDateTime,
    val hasActiveRecall: Boolean = false,
    val isInspectionOverdue: Boolean = false,
    val overdueItemCount: Int = 0,
    val isInspectionDueSoon: Boolean = false,
    val inspectionDaysLeft: Long? = null,
    val lastRecordDate: LocalDate? = null,
    val mileage: MileageReading? = null,
    val firstRegistrationDate: LocalDate? = null,
    val isFirstVisit: Boolean = false,
    val previousVisitDate: LocalDate? = null,
) {
    val today: LocalDate get() = now.toLocalDate()
    val hour: Int get() = now.hour
}

/**
 * 고른 인사말. 화면은 [situation]의 문구 목록에서 [variantIndex]번째를 쓴다.
 * [number]는 문구에 넣을 값(밀린 항목 수, 남은 날, 돌파한 주행거리, 함께한 햇수)이다.
 */
data class Greeting(val situation: GreetingSituation, val seed: Long, val number: Long? = null) {
    /** 같은 날에는 몇 번을 다시 그려도 같은 문구, 다음 날에는 다음 문구가 나온다. */
    fun variantIndex(count: Int): Int = if (count <= 0) 0 else Math.floorMod(seed, count)
}

/** 규칙 하나. [number]가 문구에 넣을 값을 꺼낸다. */
class GreetingRule(
    val situation: GreetingSituation,
    val matches: (GreetingFacts) -> Boolean,
    val number: (GreetingFacts) -> Long? = { null },
)

/** [rules]를 위에서부터 보고 처음 맞는 상황을 고른다. 마지막 시간대 규칙은 늘 맞으므로 인사말이 비지 않는다. */
class GreetingEngine(private val rules: List<GreetingRule> = GreetingRules.all) {

    fun greet(facts: GreetingFacts): Greeting {
        val rule = rules.firstOrNull { it.matches(facts) } ?: GreetingRules.timeOfDayFallback
        return Greeting(rule.situation, seed = facts.today.toEpochDay(), number = rule.number(facts))
    }
}
