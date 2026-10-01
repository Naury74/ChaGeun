package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "maintenance_record",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicle_id", "item_type", "service_date")],
)
data class MaintenanceRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "service_date") val serviceDate: LocalDate,
    @ColumnInfo(name = "mileage_km") val mileageKm: Long?,
    // Null means "not entered"; 0 is a real free service and must stay distinguishable.
    @ColumnInfo(name = "cost_won") val costWon: Long?,
    @ColumnInfo(name = "shop_name") val shopName: String?,
    val memo: String?,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
