package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.model.VehicleRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVehicleRepository : VehicleRepository {
    private val primary = MutableStateFlow<Vehicle?>(null)

    val registrations = mutableListOf<VehicleRegistration>()
    var failNextRegistration = false

    override fun observePrimaryVehicle(): Flow<Vehicle?> = primary

    override suspend fun register(registration: VehicleRegistration): VehicleId {
        if (failNextRegistration) {
            failNextRegistration = false
            error("Registration failed")
        }
        registrations += registration
        val id = VehicleId("vehicle-${registrations.size}")
        primary.value = Vehicle(
            id = id,
            maker = registration.maker,
            model = registration.model,
            modelYear = registration.modelYear,
            trim = registration.trim,
            fuelType = registration.fuelType,
            firstRegistrationDate = registration.firstRegistrationDate,
            plateMasked = registration.plate?.masked,
            registrationMode = RegistrationMode.Manual,
            isPrimary = true,
        )
        return id
    }
}
