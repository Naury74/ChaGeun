package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_rule",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicle_id", "item_type", unique = true)],
)
data class MaintenanceRuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "interval_km") val intervalKm: Long?,
    @ColumnInfo(name = "interval_months") val intervalMonths: Long?,
    @ColumnInfo(name = "due_soon_km") val dueSoonKm: Long,
    @ColumnInfo(name = "due_soon_days") val dueSoonDays: Long,
    @ColumnInfo(name = "upcoming_km") val upcomingKm: Long,
    @ColumnInfo(name = "upcoming_days") val upcomingDays: Long,
    @ColumnInfo(name = "rule_source") val ruleSource: String,
    @ColumnInfo(name = "source_title") val sourceTitle: String?,
    @ColumnInfo(name = "source_url") val sourceUrl: String?,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
)
