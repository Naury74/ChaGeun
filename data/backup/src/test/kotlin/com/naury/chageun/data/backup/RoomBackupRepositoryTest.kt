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
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
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
                "f1", "v1", LocalDate.of(2026, 9, 2), 42_100, 70_000, 41_176, 1_700, "Volume", true, "Station", null,
                now, now,
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

    @Test
    fun import_restoresExportedArchive_withoutEncryptedPlate() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val archive = Uri.fromFile(File(workDir, "out.zip")).toString()
        repository.export(archive)
        repository.deleteAll()

        val preview = repository.previewImport(archive)
        val succeeded = repository.import(archive)

        assertThat(preview).isEqualTo(
            ImportPreview.Ready(
                incoming = LocalDataSummary(vehicles = 1, records = 1, photos = 1),
                current = LocalDataSummary(vehicles = 0, records = 0, photos = 0),
            ),
        )
        assertThat(succeeded).isTrue()
        assertThat(repository.summary()).isEqualTo(LocalDataSummary(vehicles = 1, records = 1, photos = 1))
        val vehicle = database.backupDao().vehicles().single()
        assertThat(vehicle.plateMasked).isEqualTo("123가 **67")
        assertThat(vehicle.plateNumberEncrypted).isNull()
        assertThat(File(attachmentDir, "a1.jpg").readText()).isEqualTo("image")
    }

    @Test
    fun import_rejectsOtherSchemaVersion_andKeepsExistingData() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val archive = zip("data.json" to """{"schema_version": 99}""")

        assertThat(repository.previewImport(archive)).isEqualTo(ImportPreview.UnsupportedVersion(99))
        assertThat(repository.import(archive)).isFalse()
        assertThat(repository.summary().vehicles).isEqualTo(1)
        assertThat(File(attachmentDir, "a1.jpg").exists()).isTrue()
    }

    @Test
    fun import_rejectsArchiveWithoutData() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val notZip = File(workDir, "note.txt").apply { writeText("hello") }

        assertThat(repository.previewImport(zip("readme.txt" to "x"))).isEqualTo(ImportPreview.Invalid)
        assertThat(repository.previewImport(Uri.fromFile(notZip).toString())).isEqualTo(ImportPreview.Invalid)
        assertThat(repository.import(Uri.fromFile(notZip).toString())).isFalse()
        assertThat(repository.summary().vehicles).isEqualTo(1)
    }

    @Test
    fun import_keepsTraversalEntriesInsideAttachmentDirectory() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val exported = File(workDir, "out.zip")
        repository.export(Uri.fromFile(exported).toString())
        val data = ZipFile(exported).use { it.getInputStream(it.getEntry("data.json")).bufferedReader().readText() }
        val archive = zip("data.json" to data, "attachments/../../escaped.jpg" to "evil")

        assertThat(repository.import(archive)).isTrue()
        assertThat(File(workDir, "escaped.jpg").exists()).isFalse()
        assertThat(File(attachmentDir, "escaped.jpg").exists()).isTrue()
    }

    private fun zip(vararg entries: Pair<String, String>): String {
        val file = Files.createTempFile(workDir.toPath(), "import", ".zip").toFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
        }
        return Uri.fromFile(file).toString()
    }
}
