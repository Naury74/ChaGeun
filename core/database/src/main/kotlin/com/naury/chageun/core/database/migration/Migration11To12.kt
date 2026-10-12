package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 앨범에서 지운 사진을 잠시 되돌릴 수 있게 지운 시각 칸을 더한다. 기존 항목은 모두 지우지 않은 상태다. */
internal object Migration11To12 : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `album_photo` ADD COLUMN `deleted_at` INTEGER")
    }
}
