package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate

/**
 * The odometer value as of [today]. History is ordered oldest first with same-day readings in entry order,
 * so a correction entered later on the same day wins over the reading it corrects.
 */
fun List<MileageReading>.currentAsOf(today: LocalDate): MileageReading? {
    val known = filter { !it.date.isAfter(today) }
    val latestDate = known.maxOfOrNull { it.date } ?: return null
    return known.last { it.date == latestDate }
}
