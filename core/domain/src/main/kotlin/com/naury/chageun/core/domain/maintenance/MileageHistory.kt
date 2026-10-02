package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate

/**
 * [today] 기준 주행거리. 이력은 오래된 순이고 같은 날 기록은 입력 순서대로 정렬되므로
 * 같은 날 나중에 입력한 보정값이 보정 대상 기록보다 우선한다.
 */
fun List<MileageReading>.currentAsOf(today: LocalDate): MileageReading? {
    val known = filter { !it.date.isAfter(today) }
    val latestDate = known.maxOfOrNull { it.date } ?: return null
    return known.last { it.date == latestDate }
}
