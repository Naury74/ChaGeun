package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceRecord
import java.time.LocalDate

data class MaintenanceEvaluationInput(
    val rule: MaintenanceRule,
    val lastService: ServiceRecord?,
    val mileageHistory: List<MileageReading>,
    val today: LocalDate,
)

/**
 * 거리 임계값과 날짜 임계값 중 먼저 도달하는 쪽으로 정비 항목을 평가한다.
 *
 * 주행거리나 정비 이력을 모르면 절대 [com.naury.chageun.core.model.MaintenanceState.Good]을 내지 않는다.
 */
fun interface MaintenanceEngine {
    fun evaluate(input: MaintenanceEvaluationInput): MaintenanceStatus
}
