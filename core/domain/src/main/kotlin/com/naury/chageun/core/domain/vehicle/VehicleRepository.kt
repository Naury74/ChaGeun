package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.model.VehicleRegistration
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observePrimaryVehicle(): Flow<Vehicle?>

    /** Newest first, with same-day readings in reverse entry order. */
    fun observeMileageLog(vehicleId: VehicleId): Flow<List<MileageEntry>>

    /**
     * Stores the vehicle, its starting mileage and default maintenance rules atomically,
     * and makes it the primary vehicle.
     */
    suspend fun register(registration: VehicleRegistration): VehicleId
}
