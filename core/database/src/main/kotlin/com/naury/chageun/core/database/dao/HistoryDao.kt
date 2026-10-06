package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.TimelineRow
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    /**
     * Timeline은 공용 이력 테이블 없이 도메인별 테이블을 모아 보여주는 projection이다.
     * [eventTypes]로 유형을 거르고, [keyword]가 비어 있지 않으면 자유 텍스트나 [matchingItemTypes] 중 하나와 일치하는 항목을 찾는다.
     */
    @Query(
        """
        SELECT event_type, id, occurred_on, title, item_type, mileage_km, cost_won, created_at, source_type FROM (
            SELECT 'Maintenance' AS event_type, id, service_date AS occurred_on, NULL AS title, item_type,
                mileage_km, cost_won, created_at, source_type,
                COALESCE(shop_name, '') || ' ' || COALESCE(memo, '') AS search_text
            FROM maintenance_record WHERE vehicle_id = :vehicleId
            UNION ALL
            SELECT 'Fuel', id, fuel_date, station_name, NULL, mileage_km, total_price_won, created_at, 'USER',
                COALESCE(station_name, '') || ' ' || COALESCE(memo, '')
            FROM fuel_record WHERE vehicle_id = :vehicleId
            UNION ALL
            SELECT kind, id, check_date, title, NULL, mileage_km, cost_won, created_at, 'USER',
                title || ' ' || COALESCE(memo, '')
            FROM check_record WHERE vehicle_id = :vehicleId
        )
        WHERE event_type IN (:eventTypes)
            AND (:keyword = '' OR search_text LIKE '%' || :keyword || '%' OR item_type IN (:matchingItemTypes))
        ORDER BY occurred_on IS NULL, occurred_on DESC, created_at DESC
        """,
    )
    fun observeTimeline(
        vehicleId: String,
        eventTypes: List<String>,
        keyword: String,
        matchingItemTypes: List<String>,
    ): Flow<List<TimelineRow>>

    @Query("SELECT * FROM fuel_record WHERE vehicle_id = :vehicleId AND id = :id")
    fun observeFuel(vehicleId: String, id: String): Flow<FuelRecordEntity?>

    @Query("SELECT * FROM check_record WHERE vehicle_id = :vehicleId AND id = :id")
    fun observeCheck(vehicleId: String, id: String): Flow<CheckRecordEntity?>

    @Query("SELECT * FROM maintenance_record WHERE vehicle_id = :vehicleId AND id = :id")
    fun observeMaintenance(vehicleId: String, id: String): Flow<MaintenanceRecordEntity?>

    @Insert
    suspend fun insertFuel(record: FuelRecordEntity)

    @Insert
    suspend fun insertCheck(record: CheckRecordEntity)

    @Query("DELETE FROM fuel_record WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun deleteFuel(vehicleId: String, id: String)

    @Query("DELETE FROM check_record WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun deleteCheck(vehicleId: String, id: String)

    @Query("DELETE FROM maintenance_record WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun deleteMaintenance(vehicleId: String, id: String)

    @Query("DELETE FROM mileage_record WHERE vehicle_id = :vehicleId AND related_record_id = :recordId")
    suspend fun deleteMileageCreatedBy(vehicleId: String, recordId: String)
}
