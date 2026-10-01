package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Adds fuel and check (inspection, repair, note) records. Existing tables are untouched. */
internal object Migration2To3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `fuel_record` (
                `id` TEXT NOT NULL,
                `vehicle_id` TEXT NOT NULL,
                `fuel_date` INTEGER NOT NULL,
                `mileage_km` INTEGER NOT NULL,
                `total_price_won` INTEGER NOT NULL,
                `volume_ml` INTEGER NOT NULL,
                `unit_price_won` INTEGER NOT NULL,
                `computed_field` TEXT,
                `is_full_tank` INTEGER NOT NULL,
                `station_name` TEXT,
                `memo` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`vehicle_id`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_fuel_record_vehicle_id_fuel_date` " +
                "ON `fuel_record` (`vehicle_id`, `fuel_date`)",
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `check_record` (
                `id` TEXT NOT NULL,
                `vehicle_id` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `check_date` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `mileage_km` INTEGER,
                `cost_won` INTEGER,
                `memo` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`vehicle_id`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_check_record_vehicle_id_check_date` " +
                "ON `check_record` (`vehicle_id`, `check_date`)",
        )
    }
}
