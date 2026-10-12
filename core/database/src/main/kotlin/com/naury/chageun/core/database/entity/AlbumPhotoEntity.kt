package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * 내 차 앨범 사진·동영상. 기록에 붙는 첨부와 달리 차량에만 속하고 찍은 날짜와 코멘트를 가진다.
 * 파일은 첨부와 같은 폴더에 두어 백업과 전체 삭제가 함께 다룬다. 동영상이면 [thumbnailName]은 앞부분에서 뽑은 장면이다.
 *
 * [mediaType]은 [MEDIA_PHOTO] 또는 [MEDIA_VIDEO]이고, [width]·[height]는 화면에 보이는 방향 기준이다.
 *
 * [deletedAt]이 있으면 지웠지만 아직 되돌릴 수 있는 항목이다. 목록·달력·백업에서는 빠지고, 되돌리기 시간이 지나거나
 * 다음에 앱을 열 때 행과 파일을 함께 지운다.
 */
@Entity(
    tableName = "album_photo",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicle_id", "taken_on")],
)
data class AlbumPhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "thumbnail_name") val thumbnailName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "taken_on") val takenOn: LocalDate,
    val comment: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "media_type", defaultValue = MEDIA_PHOTO) val mediaType: String = MEDIA_PHOTO,
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Instant? = null,
) {
    companion object {
        const val MEDIA_PHOTO = "PHOTO"
        const val MEDIA_VIDEO = "VIDEO"
    }
}
