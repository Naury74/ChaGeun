package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.PlateChange
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.model.VehicleProfileUpdate
import com.naury.chageun.core.model.VehicleRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVehicleRepository : VehicleRepository {
    private val primary = MutableStateFlow<Vehicle?>(null)

    val registrations = mutableListOf<VehicleRegistration>()
    var failNextRegistration = false

    val mileageLog = MutableStateFlow<List<MileageEntry>>(emptyList())

    override fun observePrimaryVehicle(): Flow<Vehicle?> = primary

    override fun observeMileageLog(vehicleId: VehicleId): Flow<List<MileageEntry>> = mileageLog

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

    override suspend fun updateProfile(vehicleId: VehicleId, update: VehicleProfileUpdate) {
        val current = checkNotNull(primary.value?.takeIf { it.id == vehicleId })
        primary.value = current.copy(
            maker = update.maker,
            model = update.model,
            modelYear = update.modelYear,
            fuelType = update.fuelType,
            trim = update.trim,
            plateMasked = when (val plate = update.plate) {
                PlateChange.Keep -> current.plateMasked
                PlateChange.Remove -> null
                is PlateChange.Replace -> plate.plate.masked
            },
        )
    }
}
