package com.naury.chageun.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.domain.history.MAX_SOUNDS_PER_RECORD
import com.naury.chageun.core.domain.history.MAX_SOUND_LABEL_LENGTH
import com.naury.chageun.core.domain.history.SoundAttachResult
import com.naury.chageun.core.model.AttachmentKind
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.testing.FakeSettingsRepository
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
class OfflineFirstAttachmentRepositoryTest {

    private lateinit var database: ChageunDatabase
    private val directory: File = Files.createTempDirectory("attachments").toFile()
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val vehicleId = VehicleId("v1")
    private val settings = FakeSettingsRepository()
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

    /** 실제 소리 대신 임시 파일을 쓴다. "long"은 너무 긴 녹음, "broken"은 읽을 수 없는 파일처럼 실패한다. */
    private val fakeSoundImporter = SoundImporter { uri, target, _ ->
        when {
            "broken" in uri -> SoundImport.Failed
            "long" in uri -> SoundImport.TooLong
            else -> {
                target.parentFile?.mkdirs()
                target.writeText(uri)
                SoundImport.Imported(ImportedSound(target, target.length(), durationMs = 12_000))
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

    private fun repository(dispatcher: TestDispatcher) = OfflineFirstAttachmentRepository(
        database.attachmentDao(),
        AttachmentImporters(fakeImporter, fakeSoundImporter),
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
    fun replace_keepsAttachmentIdAndOrder_andSwapsFiles() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.attach(vehicleId, owner, listOf("content://a", "content://b"))
        val before = repository.observe(vehicleId, owner).first()
        val target = before.first()

        val replaced = repository.replace(vehicleId, target.id, "file:///cache/edited.jpg")

        val after = repository.observe(vehicleId, owner).first()
        assertThat(replaced).isTrue()
        assertThat(after.map { it.id }).isEqualTo(before.map { it.id })
        val updated = after.first()
        assertThat(updated.owner).isEqualTo(owner)
        assertThat(updated.createdAt).isEqualTo(target.createdAt)
        assertThat(updated.filePath).isNotEqualTo(target.filePath)
        assertThat(File(updated.filePath).exists() && File(updated.thumbnailPath).exists()).isTrue()
        assertThat(File(target.filePath).exists() || File(target.thumbnailPath).exists()).isFalse()
        assertThat(after[1]).isEqualTo(before[1])
    }

    @Test
    fun replace_keepsOldPhoto_whenEditedPhotoCannotBeRead() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.attach(vehicleId, owner, listOf("content://a"))
        val before = repository.observe(vehicleId, owner).first().single()

        val replaced = repository.replace(vehicleId, before.id, "file:///cache/broken.jpg")

        assertThat(replaced).isFalse()
        assertThat(repository.observe(vehicleId, owner).first().single()).isEqualTo(before)
        assertThat(File(before.filePath).exists()).isTrue()
        assertThat(repository.replace(vehicleId, "missing", "file:///cache/edited.jpg")).isFalse()
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

    @Test
    fun observeBetween_placesRecordPhotosOnTheRecordDate_andSkipsVehiclePhoto() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val fuelDate = LocalDate.of(2026, 10, 12)
        database.historyDao().insertFuel(
            FuelRecordEntity(
                id = owner.id, vehicleId = "v1", fuelDate = fuelDate, mileageKm = 41_000, totalPriceWon = 68_000,
                volumeMl = 40_000, unitPriceWon = 1_700, computedField = null, isFullTank = true,
                stationName = null, memo = null, createdAt = now, updatedAt = now,
            ),
        )
        repository.attach(vehicleId, owner, listOf("content://receipt"))
        photoRepository(StandardTestDispatcher(testScheduler)).replace(vehicleId, "content://car")

        val inRange = repository.observeBetween(vehicleId, fuelDate, fuelDate).first()
        val outOfRange = repository.observeBetween(vehicleId, fuelDate.plusDays(1), fuelDate.plusDays(7)).first()

        assertThat(inRange.map { it.date }).containsExactly(fuelDate)
        assertThat(inRange.single().attachment.owner).isEqualTo(owner)
        assertThat(File(inRange.single().attachment.thumbnailPath).exists()).isTrue()
        assertThat(outOfRange).isEmpty()
    }

    @Test
    fun attachSound_storesM4a_withDurationAndNoThumbnail() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val repair = RecordRef(TimelineEventType.Repair, "repair-1")

        val result = repository.attachSound(vehicleId, repair, "file:///cache/photo_input/rec.m4a")

        val sound = repository.observe(vehicleId, repair).first().single()
        assertThat(result).isEqualTo(SoundAttachResult.Added)
        assertThat(sound.kind).isEqualTo(AttachmentKind.Sound)
        assertThat(sound.durationMs).isEqualTo(12_000)
        assertThat(sound.thumbnailPath).isEmpty()
        assertThat(File(sound.filePath).extension).isEqualTo("m4a")
        assertThat(File(sound.filePath).parentFile).isEqualTo(directory)
        assertThat(database.attachmentDao().find("v1", sound.id)?.mimeType).isEqualTo("audio/mp4")
    }

    @Test
    fun attachSound_hasItsOwnLimit_andDoesNotTakePhotoRoom() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repeat(MAX_SOUNDS_PER_RECORD) { repository.attachSound(vehicleId, owner, "file:///rec-$it.m4a") }

        val extra = repository.attachSound(vehicleId, owner, "file:///rec-extra.m4a")
        repository.attach(vehicleId, owner, List(MAX_ATTACHMENTS_PER_RECORD) { "content://$it" })

        val all = repository.observe(vehicleId, owner).first()
        assertThat(extra).isEqualTo(SoundAttachResult.LimitReached)
        assertThat(all.count { it.isSound }).isEqualTo(MAX_SOUNDS_PER_RECORD)
        assertThat(all.count { !it.isSound }).isEqualTo(MAX_ATTACHMENTS_PER_RECORD)
    }

    @Test
    fun attachSound_reportsTooLongAndFailed_withoutLeavingRows() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))

        assertThat(repository.attachSound(vehicleId, owner, "file:///long.m4a")).isEqualTo(SoundAttachResult.TooLong)
        assertThat(repository.attachSound(vehicleId, owner, "file:///broken.m4a")).isEqualTo(SoundAttachResult.Failed)

        assertThat(repository.observe(vehicleId, owner).first()).isEmpty()
        assertThat(directory.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun replaceSound_swapsFile_keepsIdAndLabel_andRemovesOldFile() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.attachSound(vehicleId, owner, "file:///first.m4a")
        val before = repository.observe(vehicleId, owner).first().single()
        repository.setLabel(vehicleId, before.id, "Engine knock")

        val result = repository.replaceSound(vehicleId, before.id, "file:///trimmed.m4a")

        val after = repository.observe(vehicleId, owner).first().single()
        assertThat(result).isEqualTo(SoundAttachResult.Added)
        assertThat(after.id).isEqualTo(before.id)
        assertThat(after.label).isEqualTo("Engine knock")
        assertThat(after.filePath).isNotEqualTo(before.filePath)
        assertThat(File(after.filePath).readText()).isEqualTo("file:///trimmed.m4a")
        assertThat(File(before.filePath).exists()).isFalse()
    }

    @Test
    fun replaceSound_keepsOldSound_whenNewOneFails_andRefusesPhotos() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.attachSound(vehicleId, owner, "file:///first.m4a")
        repository.attach(vehicleId, owner, listOf("content://photo"))
        val (sound, photo) = repository.observe(vehicleId, owner).first().partition { it.isSound }

        assertThat(repository.replaceSound(vehicleId, sound.single().id, "file:///long.m4a"))
            .isEqualTo(SoundAttachResult.TooLong)
        assertThat(repository.replaceSound(vehicleId, photo.single().id, "file:///new.m4a"))
            .isEqualTo(SoundAttachResult.Failed)
        assertThat(File(sound.single().filePath).exists()).isTrue()
    }

    @Test
    fun setLabel_trimsAndClearsBlank() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.attachSound(vehicleId, owner, "file:///rec.m4a")
        val id = repository.observe(vehicleId, owner).first().single().id

        repository.setLabel(vehicleId, id, "  Brake\nsqueal  " + "x".repeat(MAX_SOUND_LABEL_LENGTH))
        val long = repository.observe(vehicleId, owner).first().single().label
        repository.setLabel(vehicleId, id, "   ")

        assertThat(long).startsWith("Brake squeal")
        assertThat(long).hasLength(MAX_SOUND_LABEL_LENGTH)
        assertThat(repository.observe(vehicleId, owner).first().single().label).isNull()
    }

    @Test
    fun deleteSound_removesRowAndFile_withoutTouchingTheFolder() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.attachSound(vehicleId, owner, "file:///rec.m4a")
        repository.attach(vehicleId, owner, listOf("content://photo"))
        val sound = repository.observe(vehicleId, owner).first().single { it.isSound }

        repository.delete(vehicleId, sound.id)

        assertThat(repository.observe(vehicleId, owner).first().none { it.isSound }).isTrue()
        assertThat(File(sound.filePath).exists()).isFalse()
        assertThat(directory.exists()).isTrue()
        assertThat(directory.listFiles().orEmpty()).isNotEmpty()
    }

    @Test
    fun observeBetween_leavesSoundsOut() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        val fuelDate = LocalDate.of(2026, 10, 12)
        database.historyDao().insertFuel(
            FuelRecordEntity(
                id = owner.id, vehicleId = "v1", fuelDate = fuelDate, mileageKm = 41_000, totalPriceWon = 68_000,
                volumeMl = 40_000, unitPriceWon = 1_700, computedField = null, isFullTank = true,
                stationName = null, memo = null, createdAt = now, updatedAt = now,
            ),
        )
        repository.attachSound(vehicleId, owner, "file:///rec.m4a")

        assertThat(repository.observeBetween(vehicleId, fuelDate, fuelDate).first()).isEmpty()
    }

    private fun photoRepository(
        dispatcher: TestDispatcher,
        cutter: SubjectCutter = SubjectCutter { _, _, _ ->
            CutoutResult.NoSubject
        },
    ) = OfflineFirstVehiclePhotoRepository(
        database.attachmentDao(),
        VehiclePhotoImages(fakeImporter, cutter, settings),
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
        val photos = photoRepository(StandardTestDispatcher(testScheduler)) { _, target, _ ->
            target.writeText("png")
            CutoutResult.Success
        }
        photos.replace(vehicleId, "content://car")
        testScheduler.advanceUntilIdle()
        val path = checkNotNull(photos.observe(vehicleId).first())

        assertThat(path).endsWith("_cutout.png")

        photos.clear(vehicleId)

        assertThat(File(path).exists()).isFalse()
    }

    @Test
    fun vehiclePhoto_keepsOriginal_whenCutoutFails() = runTest {
        val photos = photoRepository(StandardTestDispatcher(testScheduler)) { _, _, _ -> error("model not ready") }

        assertThat(photos.replace(vehicleId, "content://car")).isTrue()
        testScheduler.advanceUntilIdle()
        assertThat(photos.observe(vehicleId).first()).endsWith(".jpg")
    }
}
