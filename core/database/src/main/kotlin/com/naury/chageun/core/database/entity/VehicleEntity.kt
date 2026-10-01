package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "vehicle")
data class VehicleEntity(
    @PrimaryKey val id: String,
    // SECURITY: Plate number and VIN are stored only as ciphertext from the Keystore-backed cipher.
    @ColumnInfo(name = "plate_number_encrypted") val plateNumberEncrypted: String?,
    @ColumnInfo(name = "plate_masked") val plateMasked: String?,
    val maker: String,
    val model: String,
    @ColumnInfo(name = "model_year") val modelYear: Int?,
    val trim: String?,
    @ColumnInfo(name = "fuel_type") val fuelType: String?,
    @ColumnInfo(name = "first_registration_date") val firstRegistrationDate: LocalDate?,
    @ColumnInfo(name = "vin_encrypted") val vinEncrypted: String?,
    @ColumnInfo(name = "registration_mode") val registrationMode: String,
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
