package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 소유자가 정비·주유·점검 기록 중 하나인 다형 구조라 소유자에 대한 foreign key가 없다.
 * 따라서 기록을 지울 때 첨부도 명시적으로 지워야 한다.
 *
 * [mediaType]은 [MEDIA_PHOTO] 또는 [MEDIA_AUDIO]다. 소리는 썸네일이 없어 [thumbnailName]이 빈 문자열이고,
 * [durationMs]·[label]은 소리에만 쓴다.
 */
@Entity(
    tableName = "attachment",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicle_id"), Index("owner_type", "owner_id")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "owner_type") val ownerType: String,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "thumbnail_name") val thumbnailName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "media_type", defaultValue = MEDIA_PHOTO) val mediaType: String = MEDIA_PHOTO,
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    val label: String? = null,
) {
    companion object {
        const val MEDIA_PHOTO = "PHOTO"
        const val MEDIA_AUDIO = "AUDIO"
    }
}
