package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 앨범에 동영상을 넣을 수 있게 종류·길이·크기 칸을 더한다. 기존 항목은 모두 사진이다. */
internal object Migration9To10 : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `album_photo` ADD COLUMN `media_type` TEXT NOT NULL DEFAULT 'PHOTO'")
        connection.execSQL("ALTER TABLE `album_photo` ADD COLUMN `duration_ms` INTEGER")
        connection.execSQL("ALTER TABLE `album_photo` ADD COLUMN `width` INTEGER")
        connection.execSQL("ALTER TABLE `album_photo` ADD COLUMN `height` INTEGER")
    }
}
