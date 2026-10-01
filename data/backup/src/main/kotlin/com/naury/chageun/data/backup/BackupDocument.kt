package com.naury.chageun.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Export format. Bump [SCHEMA_VERSION] on any incompatible change; import only accepts the same version.
 * Dates are ISO-8601 strings and amounts are integers (won, km, mL) so the file is readable without the app.
 */
@Serializable
internal data class BackupDocument(
    @SerialName("schema_version") val schemaVersion: Int = SCHEMA_VERSION,
    @SerialName("exported_at") val exportedAt: String,
    val vehicles: List<VehicleDto>,
    val mileage: List<MileageDto>,
    @SerialName("maintenance_rules") val maintenanceRules: List<RuleDto>,
    @SerialName("maintenance_records") val maintenanceRecords: List<MaintenanceDto>,
    @SerialName("fuel_records") val fuelRecords: List<FuelDto>,
    @SerialName("check_records") val checkRecords: List<CheckDto>,
    val attachments: List<AttachmentDto>,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** The plate is exported masked only; the encrypted value is tied to this device's Keystore key. */
@Serializable
internal data class VehicleDto(
    val id: String,
    val maker: String,
    val model: String,
    @SerialName("model_year") val modelYear: Int?,
    val trim: String?,
    @SerialName("fuel_type") val fuelType: String?,
    @SerialName("first_registration_date") val firstRegistrationDate: String?,
    @SerialName("plate_masked") val plateMasked: String?,
    @SerialName("registration_mode") val registrationMode: String,
)

@Serializable
internal data class MileageDto(
    val id: String,
    @SerialName("vehicle_id") val vehicleId: String,
    @SerialName("mileage_km") val mileageKm: Long,
    @SerialName("recorded_on") val recordedOn: String,
    val source: String,
)

@Serializable
internal data class RuleDto(
    @SerialName("vehicle_id") val vehicleId: String,
    val item: String,
    @SerialName("interval_km") val intervalKm: Long?,
    @SerialName("interval_months") val intervalMonths: Long?,
    val source: String,
    val enabled: Boolean,
)

@Serializable
internal data class MaintenanceDto(
    val id: String,
    @SerialName("vehicle_id") val vehicleId: String,
    val item: String,
    val date: String?,
    @SerialName("mileage_km") val mileageKm: Long?,
    @SerialName("cost_won") val costWon: Long?,
    val shop: String?,
    val memo: String?,
)

@Serializable
internal data class FuelDto(
    val id: String,
    @SerialName("vehicle_id") val vehicleId: String,
    val date: String,
    @SerialName("mileage_km") val mileageKm: Long,
    @SerialName("total_won") val totalWon: Long,
    @SerialName("volume_ml") val volumeMl: Long,
    @SerialName("unit_price_won") val unitPriceWon: Long,
    @SerialName("full_tank") val fullTank: Boolean,
    val station: String?,
    val memo: String?,
)

@Serializable
internal data class CheckDto(
    val id: String,
    @SerialName("vehicle_id") val vehicleId: String,
    val kind: String,
    val date: String,
    val title: String,
    @SerialName("mileage_km") val mileageKm: Long?,
    @SerialName("cost_won") val costWon: Long?,
    val memo: String?,
)

@Serializable
internal data class AttachmentDto(
    @SerialName("owner_type") val ownerType: String,
    @SerialName("owner_id") val ownerId: String,
    val file: String,
)
