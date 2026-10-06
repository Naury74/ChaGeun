package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.model.VehicleProfileUpdate
import com.naury.chageun.core.model.VehicleRegistration
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observePrimaryVehicle(): Flow<Vehicle?>

    /** 최신순. 같은 날 기록은 입력 역순으로 정렬한다. */
    fun observeMileageLog(vehicleId: VehicleId): Flow<List<MileageEntry>>

    /**
     * 차량, 시작 주행거리, 기본 정비 규칙을 원자적으로 저장하고
     * 해당 차량을 대표 차량으로 지정한다.
     */
    suspend fun register(registration: VehicleRegistration): VehicleId

    /** 차량 정보만 바꾼다. 기록·주행거리·사진·정비 규칙은 그대로 둔다. */
    suspend fun updateProfile(vehicleId: VehicleId, update: VehicleProfileUpdate)
}
