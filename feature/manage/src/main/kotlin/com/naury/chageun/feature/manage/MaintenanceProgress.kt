package com.naury.chageun.feature.manage

import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceStatus

/**
 * 주기 중 이미 사용한 비율. 두 기준 중 더 많이 진행된 쪽 값을 쓴다.
 * UI가 잘못된 막대를 그리지 않도록 두 기준 모두 평가할 수 없으면 null을 반환한다.
 */
internal fun usedFraction(status: MaintenanceStatus, rule: MaintenanceRule?): Float? {
    val byDistance = rule?.intervalKm?.let { interval -> status.remainingKm?.let { 1f - it.toFloat() / interval } }
    val byTime = rule?.intervalMonths?.let { months ->
        status.remainingDays?.let { 1f - it.toFloat() / (months * AVERAGE_DAYS_PER_MONTH) }
    }
    return listOfNotNull(byDistance, byTime).maxOrNull()?.coerceIn(0f, 1f)
}

private const val AVERAGE_DAYS_PER_MONTH = 30.44f
