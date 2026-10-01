package com.naury.chageun.data.vehicle

import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface VehicleDataModule {
    @Binds
    fun bindVehicleRepository(repository: OfflineFirstVehicleRepository): VehicleRepository

    @Binds
    fun bindInspectionRepository(repository: RoomInspectionRepository): InspectionRepository
}
