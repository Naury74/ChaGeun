package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVehiclePhotoRepository : VehiclePhotoRepository {
    val photo = MutableStateFlow<String?>(null)
    var importSucceeds = true

    override fun observe(vehicleId: VehicleId): Flow<String?> = photo

    override suspend fun replace(vehicleId: VehicleId, sourceUri: String): Boolean {
        if (importSucceeds) photo.value = "/photos/$sourceUri.jpg"
        return importSucceeds
    }

    override suspend fun clear(vehicleId: VehicleId) {
        photo.value = null
    }
}
