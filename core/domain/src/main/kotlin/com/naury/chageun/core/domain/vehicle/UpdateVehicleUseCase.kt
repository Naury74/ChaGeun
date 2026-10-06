package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.domain.maintenance.DefaultMaintenanceRules
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleProfileUpdate
import javax.inject.Inject

class UpdateVehicleUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val validator: RegistrationValidator,
) {
    /** @return 비어 있으면 저장했다는 뜻이다. */
    suspend operator fun invoke(vehicle: Vehicle, update: VehicleProfileUpdate): Set<RegistrationError> {
        val errors = validator.validate(update.maker, update.model, update.modelYear)
        if (errors.isNotEmpty()) return errors
        vehicleRepository.updateProfile(vehicle.id, update)
        val previousFuel = vehicle.fuelType ?: FuelType.Gasoline
        if (previousFuel != update.fuelType) syncRulesWithFuel(vehicle, previousFuel, update.fuelType)
        return emptySet()
    }

    /**
     * 연료가 바뀌면 해당하지 않게 된 항목은 끄고, 새로 해당하는 항목은 켠다.
     * 연료와 관계없는 항목과 사용자가 바꾼 주기는 건드리지 않는다.
     */
    private suspend fun syncRulesWithFuel(vehicle: Vehicle, from: FuelType, to: FuelType) {
        MaintenanceItem.entries.forEach { item ->
            val wasApplicable = DefaultMaintenanceRules.isApplicable(item, from)
            val isApplicable = DefaultMaintenanceRules.isApplicable(item, to)
            if (wasApplicable == isApplicable) return@forEach
            val rule = maintenanceRepository.findRule(vehicle.id, item)
            when {
                rule == null -> DefaultMaintenanceRules.genericFor(item, to)
                    ?.takeIf { isApplicable }
                    ?.let { maintenanceRepository.saveRule(vehicle.id, it) }
                rule.isEnabled != isApplicable -> maintenanceRepository.saveRule(
                    vehicle.id,
                    rule.copy(isEnabled = isApplicable),
                )
            }
        }
    }
}
