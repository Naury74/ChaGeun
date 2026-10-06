package com.naury.chageun.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.model.VehicleId
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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
class OfflineFirstAlbumRepositoryTest {
    private lateinit var database: ChageunDatabase
    private val directory: File = Files.createTempDirectory("album").toFile()
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val today = LocalDate.of(2026, 10, 1)
    private val vehicleId = VehicleId("v1")
    private val silentLogger = object : AppLogger {
        override fun debug(event: String, vararg fields: LogField) = Unit

        override fun warn(event: String, vararg fields: LogField, error: Throwable?) = Unit

        override fun error(event: String, vararg fields: LogField, error: Throwable?) = Unit
    }

    /** "dated"가 들어간 URI는 EXIF 촬영일이 있는 사진처럼, "broken"은 읽을 수 없는 사진처럼 동작한다. */
    private val fakeImporter = ImageImporter { uri, target, thumbnail, _ ->
        if ("broken" in uri) {
            null
        } else {
            target.parentFile?.mkdirs()
            target.writeText("image")
            thumbnail.writeText("thumb")
            ImportedImage(target, thumbnail, target.length(), LocalDate.of(2025, 7, 1).takeIf { "dated" in uri })
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

    private fun repository(dispatcher: TestDispatcher) = OfflineFirstAlbumRepository(
        database.albumDao(),
        fakeImporter,
        directory,
        Clock.fixed(now, ZoneOffset.UTC),
        silentLogger,
        dispatcher,
    )

    @Test
    fun add_usesExifDate_orFallback_andSortsNewestFirst() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))

        val result = repository.add(vehicleId, listOf("content://dated", "content://plain", "content://broken"), today)

        assertThat(result.addedIds).hasSize(2)
        assertThat(result.failed).isEqualTo(1)
        val photos = repository.observe(vehicleId).first()
        assertThat(photos.map { it.takenOn }).containsExactly(today, LocalDate.of(2025, 7, 1)).inOrder()
        assertThat(photos.all { File(it.filePath).isFile }).isTrue()
    }

    @Test
    fun updateDetails_trimsComment_andDeleteRemovesFiles() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val id = repository.add(vehicleId, listOf("content://plain"), today).addedIds.single()

        repository.updateDetails(vehicleId, id, LocalDate.of(2026, 9, 30), "  New tires  ")
        val updated = repository.observe(vehicleId).first().single()
        repository.delete(vehicleId, id)

        assertThat(updated.comment).isEqualTo("New tires")
        assertThat(updated.takenOn).isEqualTo(LocalDate.of(2026, 9, 30))
        assertThat(repository.observe(vehicleId).first()).isEmpty()
        assertThat(File(updated.filePath).exists()).isFalse()
        assertThat(File(updated.thumbnailPath).exists()).isFalse()
    }
}
