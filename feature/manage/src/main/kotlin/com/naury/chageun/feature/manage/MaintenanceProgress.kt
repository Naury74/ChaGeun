package com.naury.chageun.feature.manage

import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceStatus

/**
 * Share of the interval already used, taken from whichever dimension is further along.
 * Returns null when neither dimension could be evaluated, so the UI never draws a misleading bar.
 */
internal fun usedFraction(status: MaintenanceStatus, rule: MaintenanceRule?): Float? {
    val byDistance = rule?.intervalKm?.let { interval -> status.remainingKm?.let { 1f - it.toFloat() / interval } }
    val byTime = rule?.intervalMonths?.let { months ->
        status.remainingDays?.let { 1f - it.toFloat() / (months * AVERAGE_DAYS_PER_MONTH) }
    }
    return listOfNotNull(byDistance, byTime).maxOrNull()?.coerceIn(0f, 1f)
}

private const val AVERAGE_DAYS_PER_MONTH = 30.44f
