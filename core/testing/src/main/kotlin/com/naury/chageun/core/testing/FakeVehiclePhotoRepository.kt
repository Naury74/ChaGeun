package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVehiclePhotoRepository : VehiclePhotoRepository {
    val photo = MutableStateFlow<String?>(null)
    val cutout = MutableStateFlow<CutoutStatus>(CutoutStatus.Idle)
    var importSucceeds = true
    var removeBackgroundCalls = 0

    override fun observe(vehicleId: VehicleId): Flow<String?> = photo

    override fun observeCutout(vehicleId: VehicleId): Flow<CutoutStatus> = cutout

    override suspend fun replace(vehicleId: VehicleId, sourceUri: String): Boolean {
        if (importSucceeds) photo.value = "/photos/$sourceUri.jpg"
        return importSucceeds
    }

    val replacedInBackground = mutableListOf<Pair<VehicleId, String>>()

    override fun replaceInBackground(vehicleId: VehicleId, sourceUri: String) {
        replacedInBackground += vehicleId to sourceUri
    }

    override fun removeBackground(vehicleId: VehicleId) {
        removeBackgroundCalls++
    }

    override suspend fun clear(vehicleId: VehicleId) {
        photo.value = null
    }
}
