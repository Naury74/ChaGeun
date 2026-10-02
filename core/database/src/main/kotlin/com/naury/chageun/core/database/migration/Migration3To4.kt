package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 이력 기록용 사진·영수증 첨부를 추가한다. */
internal object Migration3To4 : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `attachment` (
                `id` TEXT NOT NULL,
                `vehicle_id` TEXT NOT NULL,
                `owner_type` TEXT NOT NULL,
                `owner_id` TEXT NOT NULL,
                `file_name` TEXT NOT NULL,
                `thumbnail_name` TEXT NOT NULL,
                `mime_type` TEXT NOT NULL,
                `size_bytes` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`vehicle_id`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_attachment_vehicle_id` ON `attachment` (`vehicle_id`)")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachment_owner_type_owner_id` " +
                "ON `attachment` (`owner_type`, `owner_id`)",
        )
    }
}
