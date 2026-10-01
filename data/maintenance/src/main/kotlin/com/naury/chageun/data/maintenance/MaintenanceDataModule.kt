package com.naury.chageun.data.maintenance

import com.naury.chageun.core.domain.maintenance.MaintenanceEngine
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.reminder.ReminderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MaintenanceDataModule {
    @Binds
    fun bindMaintenanceRepository(repository: OfflineFirstMaintenanceRepository): MaintenanceRepository

    @Binds
    fun bindMaintenanceEngine(engine: RuleBasedMaintenanceEngine): MaintenanceEngine

    @Binds
    fun bindReminderRepository(repository: RoomReminderRepository): ReminderRepository
}
