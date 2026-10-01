package com.naury.chageun.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
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
