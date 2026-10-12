package com.naury.chageun.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.model.AlbumMediaType
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

    /** ".mp4"로 끝나는 URI는 동영상이다. "long"이 들어가면 너무 긴 동영상, "corrupt"면 읽을 수 없는 동영상이다. */
    private val fakeVideoImporter = object : VideoImporter {
        override fun isVideo(sourceUri: String) = sourceUri.endsWith(".mp4")

        override fun import(
            sourceUri: String,
            target: File,
            poster: File,
            maxDurationMs: Long,
            maxBytes: Long,
        ): VideoImport = when {
            "long" in sourceUri -> VideoImport.TooLarge
            "corrupt" in sourceUri -> VideoImport.Failed
            else -> {
                target.parentFile?.mkdirs()
                target.writeText("video")
                poster.writeText("poster")
                VideoImport.Imported(
                    ImportedVideo(target, poster, target.length(), 42_000, 1080, 1920, LocalDate.of(2025, 8, 15)),
                )
            }
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
        AlbumImporters(fakeImporter, fakeVideoImporter),
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

    @Test
    fun add_video_keepsPosterDurationAndSize_andCountsTooLargeSeparately() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))

        val result = repository.add(
            vehicleId,
            listOf("content://clip.mp4", "content://long.mp4", "content://corrupt.mp4", "content://plain"),
            today,
        )

        assertThat(result.addedIds).hasSize(2)
        assertThat(result.tooLarge).isEqualTo(1)
        assertThat(result.failed).isEqualTo(1)
        val video = repository.observe(vehicleId).first().single { it.isVideo }
        assertThat(video.mediaType).isEqualTo(AlbumMediaType.Video)
        assertThat(video.durationMs).isEqualTo(42_000)
        assertThat(video.width to video.height).isEqualTo(1080 to 1920)
        assertThat(video.takenOn).isEqualTo(LocalDate.of(2025, 8, 15))
        assertThat(video.filePath).endsWith(".mp4")
        assertThat(File(video.thumbnailPath).readText()).isEqualTo("poster")
        assertThat(directory.listFiles()!!.map { it.name }.filter { "long" in it || "corrupt" in it }).isEmpty()
    }

    @Test
    fun replace_keepsIdDateAndNote_swapsFiles_andDeletesOldOnes() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val id = repository.add(vehicleId, listOf("content://plain"), today).addedIds.single()
        repository.updateDetails(vehicleId, id, LocalDate.of(2026, 9, 30), "Road trip")
        val before = repository.observe(vehicleId).first().single()

        val replaced = repository.replace(vehicleId, id, "file:///cache/edited.mp4")

        val after = repository.observe(vehicleId).first().single()
        assertThat(replaced).isTrue()
        assertThat(after.id).isEqualTo(id)
        assertThat(after.takenOn).isEqualTo(LocalDate.of(2026, 9, 30))
        assertThat(after.comment).isEqualTo("Road trip")
        assertThat(after.createdAt).isEqualTo(before.createdAt)
        assertThat(after.isVideo).isTrue()
        assertThat(after.filePath).isNotEqualTo(before.filePath)
        assertThat(File(before.filePath).exists()).isFalse()
        assertThat(File(before.thumbnailPath).exists()).isFalse()
        assertThat(File(after.filePath).isFile).isTrue()
    }

    @Test
    fun replace_failure_keepsTheOriginal() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val id = repository.add(vehicleId, listOf("content://plain"), today).addedIds.single()
        val before = repository.observe(vehicleId).first().single()

        assertThat(repository.replace(vehicleId, id, "content://broken")).isFalse()
        assertThat(repository.replace(vehicleId, id, "content://long.mp4")).isFalse()
        assertThat(repository.replace(vehicleId, "missing", "content://plain")).isFalse()

        assertThat(repository.observe(vehicleId).first().single()).isEqualTo(before)
        assertThat(File(before.filePath).isFile).isTrue()
    }

    @Test
    fun delete_video_removesVideoAndPoster() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val id = repository.add(vehicleId, listOf("content://clip.mp4"), today).addedIds.single()
        val video = repository.observe(vehicleId).first().single()

        repository.delete(vehicleId, id)

        assertThat(File(video.filePath).exists()).isFalse()
        assertThat(File(video.thumbnailPath).exists()).isFalse()
        assertThat(directory.listFiles().orEmpty()).isEmpty()
    }
}
