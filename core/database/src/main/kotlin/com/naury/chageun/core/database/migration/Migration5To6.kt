package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 차량별 다음 검사일(지금은 사용자 입력, 이후 공식 데이터)을 추가한다. */
internal object Migration5To6 : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inspection_schedule` (
                `vehicle_id` TEXT NOT NULL,
                `next_due_date` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `notified_stage` TEXT,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`vehicle_id`),
                FOREIGN KEY(`vehicle_id`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
    }
}
