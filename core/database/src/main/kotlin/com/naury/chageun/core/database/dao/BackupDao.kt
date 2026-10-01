package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity

/** Whole-database reads and wipe used by export and "delete all data". */
@Dao
interface BackupDao {
    @Query("SELECT * FROM vehicle")
    suspend fun vehicles(): List<VehicleEntity>

    @Query("SELECT * FROM mileage_record ORDER BY recorded_on, created_at")
    suspend fun mileage(): List<MileageRecordEntity>

    @Query("SELECT * FROM maintenance_rule")
    suspend fun rules(): List<MaintenanceRuleEntity>

    @Query("SELECT * FROM maintenance_record ORDER BY service_date, created_at")
    suspend fun maintenanceRecords(): List<MaintenanceRecordEntity>

    @Query("SELECT * FROM fuel_record ORDER BY fuel_date, created_at")
    suspend fun fuelRecords(): List<FuelRecordEntity>

    @Query("SELECT * FROM check_record ORDER BY check_date, created_at")
    suspend fun checkRecords(): List<CheckRecordEntity>

    @Query("SELECT * FROM attachment ORDER BY created_at")
    suspend fun attachments(): List<AttachmentEntity>

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM maintenance_record) + (SELECT COUNT(*) FROM fuel_record)
            + (SELECT COUNT(*) FROM check_record)
        """,
    )
    suspend fun recordCount(): Int

    /** Every other table cascades from vehicle. */
    @Query("DELETE FROM vehicle")
    suspend fun deleteAllVehicles()
}
