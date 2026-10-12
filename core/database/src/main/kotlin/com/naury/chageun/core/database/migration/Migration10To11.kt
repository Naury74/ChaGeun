package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 기록에 녹음한 소리를 붙일 수 있게 종류·길이·이름 칸을 더한다. 기존 첨부는 모두 사진이다. */
internal object Migration10To11 : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `attachment` ADD COLUMN `media_type` TEXT NOT NULL DEFAULT 'PHOTO'")
        connection.execSQL("ALTER TABLE `attachment` ADD COLUMN `duration_ms` INTEGER")
        connection.execSQL("ALTER TABLE `attachment` ADD COLUMN `label` TEXT")
    }
}
