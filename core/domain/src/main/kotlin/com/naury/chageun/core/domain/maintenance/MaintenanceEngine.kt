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
 * Evaluates a maintenance item using the earliest of the distance or date threshold.
 *
 * Unknown mileage or service history never produces [com.naury.chageun.core.model.MaintenanceState.Good].
 */
fun interface MaintenanceEngine {
    fun evaluate(input: MaintenanceEvaluationInput): MaintenanceStatus
}
