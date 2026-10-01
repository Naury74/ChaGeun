package com.naury.chageun.data.backup

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.ZipFile
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomBackupRepositoryTest {

    private lateinit var database: ChageunDatabase
    private val workDir: File = Files.createTempDirectory("backup").toFile()
    private val attachmentDir = File(workDir, "attachments").apply { mkdirs() }
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val silentLogger = object : AppLogger {
        override fun debug(event: String, vararg fields: LogField) = Unit

        override fun warn(event: String, vararg fields: LogField, error: Throwable?) = Unit

        override fun error(event: String, vararg fields: LogField, error: Throwable?) = Unit
    }

    @Before
    fun setUp() = runTest {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChageunDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        database.vehicleDao().upsert(
            VehicleEntity(
                id = "v1", plateNumberEncrypted = "ciphertext-123가4567", plateMasked = "123가 **67", maker = "Maker",
                model = "Model", modelYear = 2023, trim = null, fuelType = "Gasoline", firstRegistrationDate = null,
                vinEncrypted = null, registrationMode = "Manual", isPrimary = true, createdAt = now, updatedAt = now,
            ),
        )
        database.mileageRecordDao().insert(
            MileageRecordEntity("m1", "v1", 42_000, LocalDate.of(2026, 9, 1), "USER", null, now),
        )
        database.historyDao().insertFuel(
            FuelRecordEntity(
                "f1", "v1",
                LocalDate.of(
                    2026,
                    9,
                    2,
                ),
                42_100, 70_000, 41_176, 1_700, "Volume", true, "Station", null, now, now,
            ),
        )
        File(attachmentDir, "a1.jpg").writeText("image")
        database.attachmentDao().insert(
            AttachmentEntity("a1", "v1", "Fuel", "f1", "a1.jpg", "a1_thumb.jpg", "image/jpeg", 5, now),
        )
    }

    @After
    fun tearDown() {
        database.close()
        workDir.deleteRecursively()
    }

    private fun repository(dispatcher: TestDispatcher) = RoomBackupRepository(
        ApplicationProvider.getApplicationContext(),
        database,
        attachmentDir,
        Clock.fixed(now, ZoneOffset.UTC),
        silentLogger,
        dispatcher,
    )

    @Test
    fun summary_countsVehiclesRecordsAndPhotos() = runTest {
        val summary = repository(StandardTestDispatcher(testScheduler)).summary()

        assertThat(summary.vehicles).isEqualTo(1)
        assertThat(summary.records).isEqualTo(1)
        assertThat(summary.photos).isEqualTo(1)
    }

    @Test
    fun export_writesJsonAndImages_withoutEncryptedPlate() = runTest {
        val target = File(workDir, "out.zip")

        val succeeded = repository(StandardTestDispatcher(testScheduler)).export(Uri.fromFile(target).toString())

        assertThat(succeeded).isTrue()
        ZipFile(target).use { zip ->
            val json = zip.getInputStream(zip.getEntry("data.json")).bufferedReader().readText()
            assertThat(json).contains("\"schema_version\": 1")
            assertThat(json).contains("\"plate_masked\": \"123가 **67\"")
            assertThat(json).doesNotContain("ciphertext")
            assertThat(json).contains("\"volume_ml\": 41176")
            assertThat(zip.getEntry("attachments/a1.jpg")).isNotNull()
        }
    }

    @Test
    fun deleteAll_clearsDatabaseAndFiles() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))

        repository.deleteAll()

        assertThat(repository.summary().vehicles).isEqualTo(0)
        assertThat(repository.summary().records).isEqualTo(0)
        assertThat(attachmentDir.exists()).isFalse()
    }
}
