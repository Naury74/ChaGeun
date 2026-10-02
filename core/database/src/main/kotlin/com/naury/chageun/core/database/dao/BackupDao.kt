package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.InspectionScheduleEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity

/** 내보내기, 가져오기, "모든 데이터 삭제"에서 쓰는 전체 DB 조회·복원·삭제. */
@Suppress("TooManyFunctions")
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

    @Query("SELECT * FROM inspection_schedule")
    suspend fun inspectionSchedules(): List<InspectionScheduleEntity>

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM maintenance_record) + (SELECT COUNT(*) FROM fuel_record)
            + (SELECT COUNT(*) FROM check_record)
        """,
    )
    suspend fun recordCount(): Int

    /** Room은 파라미터 순서대로 insert하므로 부모 행을 그 행을 참조하는 행보다 앞에 둔다. */
    @Insert
    suspend fun insertVehicleData(
        vehicles: List<VehicleEntity>,
        mileage: List<MileageRecordEntity>,
        rules: List<MaintenanceRuleEntity>,
        inspections: List<InspectionScheduleEntity>,
    )

    @Insert
    suspend fun insertRecords(
        maintenance: List<MaintenanceRecordEntity>,
        fuel: List<FuelRecordEntity>,
        checks: List<CheckRecordEntity>,
        attachments: List<AttachmentEntity>,
    )

    /** 나머지 테이블은 모두 vehicle에서 cascade로 함께 지워진다. */
    @Query("DELETE FROM vehicle")
    suspend fun deleteAllVehicles()
}
