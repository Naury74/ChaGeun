package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** `maintenance_record.service_date`를 nullable로 바꾼다. SQLite는 NOT NULL 제약을 그대로 풀 수 없어 테이블을 다시 만든다. */
internal object Migration1To2 : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE `maintenance_record_new` (
                `id` TEXT NOT NULL,
                `vehicle_id` TEXT NOT NULL,
                `item_type` TEXT NOT NULL,
                `service_date` INTEGER,
                `mileage_km` INTEGER,
                `cost_won` INTEGER,
                `shop_name` TEXT,
                `memo` TEXT,
                `source_type` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`vehicle_id`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL("INSERT INTO `maintenance_record_new` SELECT * FROM `maintenance_record`")
        connection.execSQL("DROP TABLE `maintenance_record`")
        connection.execSQL("ALTER TABLE `maintenance_record_new` RENAME TO `maintenance_record`")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_maintenance_record_vehicle_id_item_type_service_date` " +
                "ON `maintenance_record` (`vehicle_id`, `item_type`, `service_date`)",
        )
    }
}
