package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * 내 차 앨범 사진. 기록에 붙는 첨부와 달리 차량에만 속하고 찍은 날짜와 코멘트를 가진다.
 * 이미지 파일은 첨부와 같은 폴더에 두어 백업과 전체 삭제가 함께 다룬다.
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
)
