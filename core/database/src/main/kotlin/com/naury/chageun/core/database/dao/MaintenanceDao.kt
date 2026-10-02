package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_rule WHERE vehicle_id = :vehicleId AND is_enabled = 1")
    fun observeEnabledRules(vehicleId: String): Flow<List<MaintenanceRuleEntity>>

    @Query("SELECT * FROM maintenance_rule WHERE vehicle_id = :vehicleId")
    fun observeRules(vehicleId: String): Flow<List<MaintenanceRuleEntity>>

    @Upsert
    suspend fun upsertRules(rules: List<MaintenanceRuleEntity>)

    /** 항목별 최신 기록. 같은 날짜끼리는 가장 나중에 생성된 기록을 택한다. */
    @Query(
        """
        SELECT r.* FROM maintenance_record r
        WHERE r.vehicle_id = :vehicleId AND r.id = (
            SELECT r2.id FROM maintenance_record r2
            WHERE r2.vehicle_id = r.vehicle_id AND r2.item_type = r.item_type
            ORDER BY r2.service_date DESC, r2.created_at DESC
            LIMIT 1
        )
        """,
    )
    fun observeLatestRecords(vehicleId: String): Flow<List<MaintenanceRecordEntity>>

    @Query(
        """
        SELECT * FROM maintenance_record
        WHERE vehicle_id = :vehicleId AND item_type = :itemType
        ORDER BY service_date DESC, created_at DESC
        """,
    )
    fun observeRecords(vehicleId: String, itemType: String): Flow<List<MaintenanceRecordEntity>>

    @Query("SELECT * FROM maintenance_rule WHERE vehicle_id = :vehicleId AND item_type = :itemType")
    suspend fun findRule(vehicleId: String, itemType: String): MaintenanceRuleEntity?

    @Query(
        """
        SELECT * FROM maintenance_record
        WHERE vehicle_id = :vehicleId AND item_type = :itemType
        ORDER BY service_date DESC, created_at DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestRecord(vehicleId: String, itemType: String): MaintenanceRecordEntity?

    @Insert
    suspend fun insertRecord(record: MaintenanceRecordEntity)
}
