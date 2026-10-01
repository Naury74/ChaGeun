package com.naury.chageun.data.maintenance

import com.naury.chageun.core.database.dao.MaintenanceDao
import com.naury.chageun.core.database.dao.MileageRecordDao
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.model.VehicleId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class OfflineFirstMaintenanceRepository @Inject constructor(
    private val maintenanceDao: MaintenanceDao,
    private val mileageRecordDao: MileageRecordDao,
) : MaintenanceRepository {

    override fun observeInputs(vehicleId: VehicleId): Flow<MaintenanceInputs> = combine(
        maintenanceDao.observeEnabledRules(vehicleId.value),
        maintenanceDao.observeLatestRecords(vehicleId.value),
        mileageRecordDao.observeAll(vehicleId.value),
    ) { rules, latestRecords, mileage ->
        MaintenanceInputs(
            rules = rules.mapNotNull { it.asExternalModelOrNull() },
            lastServices = latestRecords
                .mapNotNull { record ->
                    record.itemType.toMaintenanceItemOrNull()?.let { it to record.asServiceRecord() }
                }
                .toMap(),
            mileageHistory = mileage.map { it.asExternalModel() },
        )
    }
}
