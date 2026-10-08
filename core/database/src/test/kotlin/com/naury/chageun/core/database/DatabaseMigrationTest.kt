package com.naury.chageun.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.migration.Migration1To2
import com.naury.chageun.core.database.migration.Migration2To3
import com.naury.chageun.core.database.migration.Migration3To4
import com.naury.chageun.core.database.migration.Migration4To5
import com.naury.chageun.core.database.migration.Migration5To6
import com.naury.chageun.core.database.migration.Migration6To7
import com.naury.chageun.core.database.migration.Migration7To8
import com.naury.chageun.core.database.migration.Migration8To9
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = ChageunDatabase::class.java,
    )

    @Test
    fun createsExportedSchemaForEveryVersion() {
        (FIRST_VERSION..currentVersion()).forEach { version ->
            helper.createDatabase(TEST_DB, version).close()
        }
    }

    @Test
    fun migratesFromFirstToCurrentVersion() {
        helper.createDatabase(TEST_DB, FIRST_VERSION).close()

        helper.runMigrationsAndValidate(TEST_DB, currentVersion(), true, *DatabaseMigrations.ALL.toTypedArray()).close()
    }

    @Test
    fun migration1To2_keepsRecordsAndAllowsUndatedService() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO vehicle (id, maker, model, registration_mode, is_primary, created_at, updated_at) " +
                    "VALUES ('v1', 'Maker', 'Model', 'Manual', 1, 0, 0)",
            )
            db.execSQL(
                "INSERT INTO maintenance_record " +
                    "(id, vehicle_id, item_type, service_date, mileage_km, cost_won, " +
                    "source_type, created_at, updated_at) " +
                    "VALUES ('r1', 'v1', 'EngineOil', 20500, 40000, 0, 'USER', 1, 1)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, Migration1To2).use { db ->
            db.query(
                "SELECT service_date, mileage_km, cost_won FROM maintenance_record WHERE id = 'r1'",
            ).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getLong(0)).isEqualTo(20_500)
                assertThat(cursor.getLong(1)).isEqualTo(40_000)
                assertThat(cursor.getLong(2)).isEqualTo(0)
            }
            db.execSQL(
                "INSERT INTO maintenance_record (id, vehicle_id, item_type, service_date, source_type, created_at, " +
                    "updated_at) VALUES ('r2', 'v1', 'Tire', NULL, 'USER', 2, 2)",
            )
        }
    }

    @Test
    fun migration2To3_keepsExistingRecordsAndAddsHistoryTables() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO vehicle (id, maker, model, registration_mode, is_primary, created_at, updated_at) " +
                    "VALUES ('v1', 'Maker', 'Model', 'Manual', 1, 0, 0)",
            )
            db.execSQL(
                "INSERT INTO maintenance_record (id, vehicle_id, item_type, service_date, source_type, created_at, " +
                    "updated_at) VALUES ('r1', 'v1', 'Tire', NULL, 'USER', 1, 1)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, Migration2To3).use { db ->
            db.query("SELECT COUNT(*) FROM maintenance_record").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(1)
            }
            db.execSQL(
                "INSERT INTO fuel_record (id, vehicle_id, fuel_date, mileage_km, total_price_won, volume_ml, " +
                    "unit_price_won, is_full_tank, created_at, updated_at) VALUES ('f1', 'v1', 20500, 42000, 70000, " +
                    "41176, 1700, 1, 2, 2)",
            )
        }
    }

    @Test
    fun migration3To4_addsAttachmentTable() {
        helper.createDatabase(TEST_DB, 3).close()

        helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3To4).close()
    }

    @Test
    fun migration4To5_addsReminderState() {
        helper.createDatabase(TEST_DB, 4).close()

        helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration4To5).close()
    }

    @Test
    fun migration5To6_addsInspectionSchedule() {
        helper.createDatabase(TEST_DB, 5).close()

        helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5To6).close()
    }

    @Test
    fun migration6To7_addsAlbumPhoto() {
        helper.createDatabase(TEST_DB, 6).close()

        helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6To7).close()
    }

    @Test
    fun migration7To8_marksOldInspectionRecordsAsPeriodic_only() {
        helper.createDatabase(TEST_DB, 7).use { db ->
            db.execSQL(
                "INSERT INTO vehicle (id, maker, model, registration_mode, is_primary, created_at, updated_at) " +
                    "VALUES ('v1', 'Maker', 'Model', 'Manual', 1, 0, 0)",
            )
            listOf(
                Triple("ko", "Inspection", "자동차 정기검사"),
                Triple("en", "Inspection", "Periodic inspection"),
                Triple("free", "Inspection", "무상 점검"),
                Triple("repair", "Repair", "Periodic inspection"),
            ).forEach { (id, kind, title) ->
                db.execSQL(
                    "INSERT INTO check_record (id, vehicle_id, kind, check_date, title, created_at, updated_at) " +
                        "VALUES ('$id', 'v1', '$kind', 20500, '$title', 0, 0)",
                )
            }
        }

        helper.runMigrationsAndValidate(TEST_DB, 8, true, Migration7To8).use { db ->
            db.query("SELECT id, periodic_result FROM check_record ORDER BY id").use { cursor ->
                val results = buildMap {
                    while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1))
                }
                assertThat(results).containsExactly("en", "Unknown", "free", null, "ko", "Unknown", "repair", null)
            }
        }
    }

    @Test
    fun migration8To9_keepsVehicleAndLeavesDisplacementEmpty() {
        helper.createDatabase(TEST_DB, 8).use { db ->
            db.execSQL(
                "INSERT INTO vehicle (id, maker, model, registration_mode, is_primary, created_at, updated_at) " +
                    "VALUES ('v1', 'Maker', 'Model', 'Manual', 1, 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 9, true, Migration8To9).use { db ->
            db.query("SELECT maker, displacement_cc FROM vehicle WHERE id = 'v1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("Maker")
                assertThat(cursor.isNull(1)).isTrue()
            }
            db.execSQL("UPDATE vehicle SET displacement_cc = 1598 WHERE id = 'v1'")
            db.query("SELECT displacement_cc FROM vehicle WHERE id = 'v1'").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(1598)
            }
        }
    }

    @Test
    fun providesMigrationForEveryVersionStep() {
        val covered = DatabaseMigrations.ALL.map { it.startVersion to it.endVersion }.toSet()
        val required = (FIRST_VERSION until currentVersion()).map { it to it + 1 }

        assertThat(covered).containsAtLeastElementsIn(required)
    }

    private fun currentVersion(): Int = ChageunDatabase.VERSION

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val FIRST_VERSION = 1
    }
}
