package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MonthlyCostRow
import com.naury.chageun.core.database.entity.TimelineRow
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

// 기록 종류(정비·주유·점검)마다 조회·추가·수정·삭제 쿼리가 따로 필요하다.
@Suppress("TooManyFunctions")
@Dao
interface HistoryDao {
    /**
     * Timeline은 공용 이력 테이블 없이 도메인별 테이블을 모아 보여주는 projection이다.
     * [eventTypes]로 유형을 거르고, [keyword]가 비어 있지 않으면 자유 텍스트나 [matchingItemTypes] 중 하나와 일치하는 항목을 찾는다.
     * 기간·비용 조건이 있으면 날짜나 비용이 없는 기록은 빠진다. 첨부 수는 attachment 테이블에서 센다.
     * 기록이 많아도 첫 화면이 빨리 뜨도록 최신순으로 [limit]개만 읽는다. -1이면 모두 읽는다.
     *
     * 아래 [observeMonthlyCosts]와 조건이 같아야 월별 합계가 목록과 맞는다. 한쪽을 고치면 다른 쪽도 고친다.
     */
    @Query(
        """
        SELECT event_type, id, occurred_on, title, item_type, mileage_km, cost_won, created_at, source_type,
            (SELECT COUNT(*) FROM attachment a WHERE a.owner_type = t.event_type AND a.owner_id = t.id) AS attachment_count
        FROM (
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
        ) AS t
        WHERE event_type IN (:eventTypes)
            AND (:keyword = '' OR search_text LIKE '%' || :keyword || '%' OR item_type IN (:matchingItemTypes))
            AND (:dateFrom IS NULL OR occurred_on >= :dateFrom)
            AND (:dateTo IS NULL OR occurred_on <= :dateTo)
            AND (:minCostWon IS NULL OR cost_won >= :minCostWon)
            AND (:maxCostWon IS NULL OR cost_won <= :maxCostWon)
            AND (:withAttachmentsOnly = 0 OR attachment_count > 0)
        ORDER BY occurred_on IS NULL, occurred_on DESC, created_at DESC
        LIMIT :limit
        """,
    )
    fun observeTimeline(
        vehicleId: String,
        eventTypes: List<String>,
        keyword: String,
        matchingItemTypes: List<String>,
        dateFrom: LocalDate? = null,
        dateTo: LocalDate? = null,
        minCostWon: Long? = null,
        maxCostWon: Long? = null,
        withAttachmentsOnly: Boolean = false,
        limit: Int = -1,
    ): Flow<List<TimelineRow>>

    /**
     * [observeTimeline]과 같은 조건에 맞는 기록 전체의 월별 비용 합계. 목록을 나눠 읽어도 월 합계는 전체 기준이다.
     * 날짜는 epoch day로 저장되므로 월은 epoch day를 날짜로 바꿔 'YYYY-MM'으로 묶는다. 날짜 없는 기록은 month가 null이다.
     */
    @Query(
        """
        SELECT strftime('%Y-%m', occurred_on * 86400, 'unixepoch') AS month, SUM(cost_won) AS total_won
        FROM (
            SELECT *, (SELECT COUNT(*) FROM attachment a WHERE a.owner_type = u.event_type AND a.owner_id = u.id)
                AS attachment_count
            FROM (
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
            ) AS u
        ) AS t
        WHERE event_type IN (:eventTypes)
            AND (:keyword = '' OR search_text LIKE '%' || :keyword || '%' OR item_type IN (:matchingItemTypes))
            AND (:dateFrom IS NULL OR occurred_on >= :dateFrom)
            AND (:dateTo IS NULL OR occurred_on <= :dateTo)
            AND (:minCostWon IS NULL OR cost_won >= :minCostWon)
            AND (:maxCostWon IS NULL OR cost_won <= :maxCostWon)
            AND (:withAttachmentsOnly = 0 OR attachment_count > 0)
        GROUP BY month
        """,
    )
    fun observeMonthlyCosts(
        vehicleId: String,
        eventTypes: List<String>,
        keyword: String,
        matchingItemTypes: List<String>,
        dateFrom: LocalDate? = null,
        dateTo: LocalDate? = null,
        minCostWon: Long? = null,
        maxCostWon: Long? = null,
        withAttachmentsOnly: Boolean = false,
    ): Flow<List<MonthlyCostRow>>

    @Query("SELECT * FROM fuel_record WHERE vehicle_id = :vehicleId AND id = :id")
    fun observeFuel(vehicleId: String, id: String): Flow<FuelRecordEntity?>

    @Query("SELECT * FROM check_record WHERE vehicle_id = :vehicleId AND id = :id")
    fun observeCheck(vehicleId: String, id: String): Flow<CheckRecordEntity?>

    @Query("SELECT * FROM maintenance_record WHERE vehicle_id = :vehicleId AND id = :id")
    fun observeMaintenance(vehicleId: String, id: String): Flow<MaintenanceRecordEntity?>

    @Query("SELECT * FROM fuel_record WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun findFuel(vehicleId: String, id: String): FuelRecordEntity?

    @Query("SELECT * FROM check_record WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun findCheck(vehicleId: String, id: String): CheckRecordEntity?

    @Query("SELECT * FROM maintenance_record WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun findMaintenance(vehicleId: String, id: String): MaintenanceRecordEntity?

    @Insert
    suspend fun insertFuel(record: FuelRecordEntity)

    @Update
    suspend fun updateFuel(record: FuelRecordEntity)

    @Update
    suspend fun updateCheck(record: CheckRecordEntity)

    @Update
    suspend fun updateMaintenance(record: MaintenanceRecordEntity)

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
