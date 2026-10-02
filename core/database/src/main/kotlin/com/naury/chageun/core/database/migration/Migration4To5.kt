package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 정비 항목별로 알림을 보낸 단계를 추가한다. */
internal object Migration4To5 : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder_state` (
                `vehicle_id` TEXT NOT NULL,
                `item_type` TEXT NOT NULL,
                `notified_state` TEXT NOT NULL,
                `notified_at` INTEGER NOT NULL,
                PRIMARY KEY(`vehicle_id`, `item_type`),
                FOREIGN KEY(`vehicle_id`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
    }
}
