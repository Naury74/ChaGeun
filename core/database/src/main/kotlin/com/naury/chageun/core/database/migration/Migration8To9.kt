package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 차량 배기량(cc) 칸을 추가한다. 기존 차량은 비워 둔다. */
internal object Migration8To9 : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `vehicle` ADD COLUMN `displacement_cc` INTEGER")
    }
}
