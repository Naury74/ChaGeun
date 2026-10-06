package com.naury.chageun.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 내보내기 형식. 호환되지 않는 변경이 있으면 [SCHEMA_VERSION]을 올린다. 가져오기는 같은 버전만 받는다.
 * 앱 없이도 파일을 읽을 수 있도록 날짜는 ISO-8601 문자열, 금액·수치는 정수(won, km, mL)로 둔다.
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
    /** 버전을 올리지 않고 추가한 필드다. 기본값이 있는 선택 항목이라 이전 version-1 파일도 그대로 가져올 수 있다. */
    @SerialName("inspection_schedules") val inspectionSchedules: List<InspectionDto> = emptyList(),
    /** 버전을 올리지 않고 추가한 필드다. 앨범이 없던 때의 파일도 빈 앨범으로 가져온다. */
    @SerialName("album_photos") val albumPhotos: List<AlbumPhotoDto> = emptyList(),
) {
    /** ZIP에 함께 담을 이미지 파일 이름. */
    val imageFiles: List<String>
        get() = attachments.flatMap { listOf(it.file, it.thumbnail) } +
            albumPhotos.flatMap { listOf(it.file, it.thumbnail) }

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** 번호판은 마스킹된 값만 내보낸다. 암호화된 값은 이 기기의 Keystore 키에 묶여 있다. */
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
    @SerialName("is_primary") val isPrimary: Boolean,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class MileageDto(
    val id: String,
    @SerialName("vehicle_id") val vehicleId: String,
    @SerialName("mileage_km") val mileageKm: Long,
    @SerialName("recorded_on") val recordedOn: String,
    val source: String,
    @SerialName("related_record_id") val relatedRecordId: String?,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class RuleDto(
    @SerialName("vehicle_id") val vehicleId: String,
    val item: String,
    @SerialName("interval_km") val intervalKm: Long?,
    @SerialName("interval_months") val intervalMonths: Long?,
    @SerialName("due_soon_km") val dueSoonKm: Long,
    @SerialName("due_soon_days") val dueSoonDays: Long,
    @SerialName("upcoming_km") val upcomingKm: Long,
    @SerialName("upcoming_days") val upcomingDays: Long,
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
    @SerialName("created_at") val createdAt: String,
    // 이전 버전 백업에는 없으므로 기본값을 둔다. 없으면 사용자 입력으로 본다.
    val source: String? = null,
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
    @SerialName("computed_field") val computedField: String?,
    @SerialName("full_tank") val fullTank: Boolean,
    val station: String?,
    val memo: String?,
    @SerialName("created_at") val createdAt: String,
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
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class AttachmentDto(
    val id: String,
    @SerialName("vehicle_id") val vehicleId: String,
    @SerialName("owner_type") val ownerType: String,
    @SerialName("owner_id") val ownerId: String,
    val file: String,
    val thumbnail: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class InspectionDto(
    @SerialName("vehicle_id") val vehicleId: String,
    @SerialName("next_due_date") val nextDueDate: String,
    val source: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class AlbumPhotoDto(
    val id: String,
    @SerialName("vehicle_id") val vehicleId: String,
    val file: String,
    val thumbnail: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("taken_on") val takenOn: String,
    val comment: String?,
    @SerialName("created_at") val createdAt: String,
)
