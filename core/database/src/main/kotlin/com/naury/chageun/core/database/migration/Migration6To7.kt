package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 내 차 앨범 사진을 추가한다. */
internal object Migration6To7 : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `album_photo` (
                `id` TEXT NOT NULL,
                `vehicle_id` TEXT NOT NULL,
                `file_name` TEXT NOT NULL,
                `thumbnail_name` TEXT NOT NULL,
                `size_bytes` INTEGER NOT NULL,
                `taken_on` INTEGER NOT NULL,
                `comment` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`vehicle_id`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_album_photo_vehicle_id_taken_on` " +
                "ON `album_photo` (`vehicle_id`, `taken_on`)",
        )
    }
}
