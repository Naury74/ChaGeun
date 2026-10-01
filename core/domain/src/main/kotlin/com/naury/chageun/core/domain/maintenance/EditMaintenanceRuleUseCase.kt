package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.VehicleId
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

enum class RuleEditError { NoInterval, NonPositiveInterval, UnknownItem }

class EditMaintenanceRuleUseCase @Inject constructor(
    private val repository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
) {
    /** Saves user-chosen intervals. The thresholds stay as they were so notifications keep their timing. */
    suspend fun update(
        vehicleId: VehicleId,
        item: MaintenanceItem,
        intervalKm: Long?,
        intervalMonths: Long?,
        isEnabled: Boolean,
    ): RuleEditError? {
        if (intervalKm == null && intervalMonths == null) return RuleEditError.NoInterval
        if ((intervalKm ?: 1) <= 0 || (intervalMonths ?: 1) <= 0) return RuleEditError.NonPositiveInterval
        val current = repository.findRule(vehicleId, item) ?: return RuleEditError.UnknownItem
        repository.saveRule(
            vehicleId,
            current.copy(
                intervalKm = intervalKm,
                intervalMonths = intervalMonths,
                isEnabled = isEnabled,
                source = RuleSource.User,
                sourceTitle = null,
                sourceUrl = null,
            ),
        )
        return null
    }

    suspend fun resetToGeneric(vehicleId: VehicleId, item: MaintenanceItem): RuleEditError? {
        val fuelType = vehicleRepository.observePrimaryVehicle().filterNotNull().first().fuelType ?: FuelType.Gasoline
        val generic = DefaultMaintenanceRules.genericFor(item, fuelType) ?: return RuleEditError.UnknownItem
        repository.saveRule(vehicleId, generic)
        return null
    }
}
