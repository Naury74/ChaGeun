package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "fuel_record",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicle_id", "fuel_date")],
)
data class FuelRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "fuel_date") val fuelDate: LocalDate,
    @ColumnInfo(name = "mileage_km") val mileageKm: Long,
    @ColumnInfo(name = "total_price_won") val totalPriceWon: Long,
    @ColumnInfo(name = "volume_ml") val volumeMl: Long,
    @ColumnInfo(name = "unit_price_won") val unitPriceWon: Long,
    @ColumnInfo(name = "computed_field") val computedField: String?,
    @ColumnInfo(name = "is_full_tank") val isFullTank: Boolean,
    @ColumnInfo(name = "station_name") val stationName: String?,
    val memo: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
