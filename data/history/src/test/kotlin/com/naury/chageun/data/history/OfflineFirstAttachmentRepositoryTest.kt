package com.naury.chageun.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleId
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstAttachmentRepositoryTest {

    private lateinit var database: ChageunDatabase
    private val directory: File = Files.createTempDirectory("attachments").toFile()
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val vehicleId = VehicleId("v1")
    private val owner = RecordRef(TimelineEventType.Fuel, "fuel-1")
    private val silentLogger = object : AppLogger {
        override fun debug(event: String, vararg fields: LogField) = Unit

        override fun warn(event: String, vararg fields: LogField, error: Throwable?) = Unit

        override fun error(event: String, vararg fields: LogField, error: Throwable?) = Unit
    }

    /** 디코딩 대신 임시 파일을 쓴다. "broken"이 포함된 URI는 읽을 수 없는 이미지처럼 실패한다. */
    private val fakeImporter = ImageImporter { uri, target, thumbnail, _ ->
        if ("broken" in uri) {
            null
        } else {
            target.parentFile?.mkdirs()
            target.writeText("image")
            thumbnail.writeText("thumb")
            ImportedImage(target, thumbnail, target.length())
        }
    }

    @Before
    fun setUp() = runTest {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChageunDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        database.vehicleDao().upsert(
            VehicleEntity(
                id = "v1", plateNumberEncrypted = null, plateMasked = null, maker = "Maker", model = "Model",
                modelYear = 2023, trim = null, fuelType = "Gasoline", firstRegistrationDate = null, vinEncrypted = null,
                registrationMode = "Manual", isPrimary = true, createdAt = now, updatedAt = now,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    private fun repository(dispatcher: TestDispatcher) = OfflineFirstAttachmentRepository(
        database.attachmentDao(),
        fakeImporter,
        directory,
        Clock.fixed(now, ZoneOffset.UTC),
        silentLogger,
        dispatcher,
    )

    @Test
    fun attach_storesFiles_andReportsFailures() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))

        val result = repository.attach(vehicleId, owner, listOf("content://a", "content://broken", "content://b"))

        val attachments = repository.observe(vehicleId, owner).first()
        assertThat(result.added).isEqualTo(2)
        assertThat(result.failed).isEqualTo(1)
        assertThat(attachments).hasSize(2)
        assertThat(attachments.all { File(it.filePath).exists() && File(it.thumbnailPath).exists() }).isTrue()
    }

    @Test
    fun attach_respectsPerRecordLimit() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))

        repository.attach(vehicleId, owner, List(MAX_ATTACHMENTS_PER_RECORD + 3) { "content://$it" })

        assertThat(repository.observe(vehicleId, owner).first()).hasSize(MAX_ATTACHMENTS_PER_RECORD)
    }

    @Test
    fun deleteAllFor_removesRowsAndFiles() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.attach(vehicleId, owner, listOf("content://a"))
        val file = File(repository.observe(vehicleId, owner).first().single().filePath)

        repository.deleteAllFor(vehicleId, owner)

        assertThat(repository.observe(vehicleId, owner).first()).isEmpty()
        assertThat(file.exists()).isFalse()
    }

    private fun photoRepository(dispatcher: TestDispatcher, cutter: SubjectCutter = SubjectCutter { _, _ -> false }) =
        OfflineFirstVehiclePhotoRepository(
            database.attachmentDao(),
            VehiclePhotoImages(fakeImporter, cutter),
            directory,
            Clock.fixed(now, ZoneOffset.UTC),
            silentLogger,
            dispatcher,
        )

    @Test
    fun vehiclePhoto_replaceKeepsOnlyLatest_andStaysOutOfRecords() = runTest {
        val photos = photoRepository(StandardTestDispatcher(testScheduler))
        photos.replace(vehicleId, "content://first")
        val first = photos.observe(vehicleId).first()

        assertThat(photos.replace(vehicleId, "content://second")).isTrue()
        val second = photos.observe(vehicleId).first()

        assertThat(second).isNotEqualTo(first)
        assertThat(File(checkNotNull(first)).exists()).isFalse()
        assertThat(File(checkNotNull(second)).exists()).isTrue()
        assertThat(repository(StandardTestDispatcher(testScheduler)).observe(vehicleId, owner).first()).isEmpty()
    }

    @Test
    fun vehiclePhoto_failedImport_keepsPrevious_andClearRemovesFiles() = runTest {
        val photos = photoRepository(StandardTestDispatcher(testScheduler))
        photos.replace(vehicleId, "content://first")
        val first = checkNotNull(photos.observe(vehicleId).first())

        assertThat(photos.replace(vehicleId, "content://broken")).isFalse()
        assertThat(photos.observe(vehicleId).first()).isEqualTo(first)

        photos.clear(vehicleId)

        assertThat(photos.observe(vehicleId).first()).isNull()
        assertThat(File(first).exists()).isFalse()
    }

    @Test
    fun vehiclePhoto_prefersCutout_andRemovesItWithThePhoto() = runTest {
        val photos = photoRepository(StandardTestDispatcher(testScheduler)) { _, target ->
            target.writeText("png")
            true
        }
        photos.replace(vehicleId, "content://car")
        val path = checkNotNull(photos.observe(vehicleId).first())

        assertThat(path).endsWith("_cutout.png")

        photos.clear(vehicleId)

        assertThat(File(path).exists()).isFalse()
    }

    @Test
    fun vehiclePhoto_keepsOriginal_whenCutoutFails() = runTest {
        val photos = photoRepository(StandardTestDispatcher(testScheduler)) { _, _ -> error("model not ready") }

        assertThat(photos.replace(vehicleId, "content://car")).isTrue()
        assertThat(photos.observe(vehicleId).first()).endsWith(".jpg")
    }
}
