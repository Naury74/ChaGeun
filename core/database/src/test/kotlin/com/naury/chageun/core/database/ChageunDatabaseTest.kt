package com.naury.chageun.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.entity.AlbumPhotoEntity
import com.naury.chageun.core.database.entity.AttachmentEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChageunDatabaseTest {

    private lateinit var database: ChageunDatabase

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChageunDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observesPrimaryVehicle() = runTest {
        database.vehicleDao().upsert(vehicle(id = "secondary", isPrimary = false))
        database.vehicleDao().upsert(vehicle(id = "primary", isPrimary = true))

        assertThat(database.vehicleDao().observePrimary().first()?.id).isEqualTo("primary")
    }

    @Test
    fun returnsLatestMileage_byRecordedDate() = runTest {
        database.vehicleDao().upsert(vehicle())
        val dao = database.mileageRecordDao()
        dao.insert(mileage("a", 41_000, LocalDate.of(2026, 9, 1)))
        dao.insert(mileage("b", 42_500, LocalDate.of(2026, 9, 30)))
        dao.insert(mileage("c", 41_800, LocalDate.of(2026, 9, 15)))

        assertThat(dao.observeLatest("vehicle-1").first()?.mileageKm).isEqualTo(42_500)
        assertThat(dao.observeSince("vehicle-1", LocalDate.of(2026, 9, 10)).first().map { it.id })
            .containsExactly("c", "b").inOrder()
    }

    @Test
    fun returnsLatestRecordPerItem() = runTest {
        database.vehicleDao().upsert(vehicle())
        val dao = database.maintenanceDao()
        dao.insertRecord(service("oil-old", "EngineOil", LocalDate.of(2025, 9, 1)))
        dao.insertRecord(service("oil-new", "EngineOil", LocalDate.of(2026, 3, 10)))
        dao.insertRecord(service("tire", "Tire", LocalDate.of(2024, 8, 5)))

        val latest = dao.observeLatestRecords("vehicle-1").first()

        assertThat(latest.map { it.id }).containsExactly("oil-new", "tire")
    }

    @Test
    fun breaksSameDayTies_byCreationTime() = runTest {
        database.vehicleDao().upsert(vehicle())
        val dao = database.maintenanceDao()
        val day = LocalDate.of(2026, 3, 10)
        dao.insertRecord(service("first", "EngineOil", day, createdAt = FIXED_NOW))
        dao.insertRecord(service("second", "EngineOil", day, createdAt = FIXED_NOW.plusSeconds(60)))

        assertThat(dao.observeLatestRecords("vehicle-1").first().single().id).isEqualTo("second")
    }

    @Test
    fun prefersDatedRecord_overUndatedOne() = runTest {
        database.vehicleDao().upsert(vehicle())
        val dao = database.maintenanceDao()
        dao.insertRecord(service("dated", "Tire", LocalDate.of(2024, 8, 5)))
        dao.insertRecord(service("undated", "Tire", on = null, createdAt = FIXED_NOW.plusSeconds(60)))

        assertThat(dao.observeLatestRecords("vehicle-1").first().single().id).isEqualTo("dated")
    }

    @Test
    fun keepsZeroCost_distinctFromMissingCost() = runTest {
        database.vehicleDao().upsert(vehicle())
        val dao = database.maintenanceDao()
        dao.insertRecord(service("free", "Wiper", LocalDate.of(2026, 1, 1), costWon = 0))
        dao.insertRecord(service("unknown", "Battery", LocalDate.of(2026, 1, 1), costWon = null))

        val costs = dao.observeLatestRecords("vehicle-1").first().associate { it.id to it.costWon }

        assertThat(costs).containsExactly("free", 0L, "unknown", null)
    }

    @Test
    fun timeline_mergesRecordTypes_newestFirst_andFilters() = runTest {
        database.vehicleDao().upsert(vehicle())
        database.maintenanceDao().insertRecord(service("oil", "EngineOil", LocalDate.of(2026, 3, 10)))
        database.maintenanceDao().insertRecord(service("tire", "Tire", on = null))
        database.historyDao().insertFuel(fuel("fuel", LocalDate.of(2026, 8, 3), station = "S-Oil"))
        database.historyDao().insertCheck(check("repair", "Repair", LocalDate.of(2026, 5, 1), "Bumper"))
        val all = TimelineEventTypes.ALL

        val timeline = database.historyDao().observeTimeline("vehicle-1", all, "", emptyList()).first()
        val fuelOnly = database.historyDao().observeTimeline("vehicle-1", listOf("Fuel"), "", emptyList()).first()
        val byKeyword = database.historyDao().observeTimeline("vehicle-1", all, "oil", emptyList()).first()
        val byItem = database.historyDao().observeTimeline("vehicle-1", all, "엔진", listOf("EngineOil")).first()

        assertThat(timeline.map { it.id }).containsExactly("fuel", "repair", "oil", "tire").inOrder()
        assertThat(fuelOnly.map { it.id }).containsExactly("fuel")
        assertThat(byKeyword.map { it.id }).containsExactly("fuel")
        assertThat(byItem.map { it.id }).containsExactly("oil")
    }

    @Test
    fun deletingRecord_removesOdometerReadingCreatedWithIt() = runTest {
        database.vehicleDao().upsert(vehicle())
        database.historyDao().insertFuel(fuel("fuel", LocalDate.of(2026, 8, 3)))
        database.mileageRecordDao().insert(
            mileage("m1", 43_000, LocalDate.of(2026, 8, 3)).copy(sourceType = "FUEL", relatedRecordId = "fuel"),
        )
        database.mileageRecordDao().insert(mileage("m0", 42_000, LocalDate.of(2026, 7, 1)))

        database.historyDao().deleteFuel("vehicle-1", "fuel")
        database.historyDao().deleteMileageCreatedBy("vehicle-1", "fuel")

        assertThat(database.mileageRecordDao().findLatest("vehicle-1")?.id).isEqualTo("m0")
    }

    @Test
    fun albumObserveBetween_includesBothEnds_inDateOrder() = runTest {
        database.vehicleDao().upsert(vehicle())
        database.vehicleDao().upsert(vehicle(id = "vehicle-2", isPrimary = false))
        val dao = database.albumDao()
        listOf(
            albumPhoto("before", LocalDate.of(2026, 9, 27)),
            albumPhoto("start", LocalDate.of(2026, 9, 28)),
            albumPhoto("middle-late", LocalDate.of(2026, 10, 12), FIXED_NOW.plusSeconds(60)),
            albumPhoto("middle-early", LocalDate.of(2026, 10, 12)),
            albumPhoto("end", LocalDate.of(2026, 11, 8)),
            albumPhoto("after", LocalDate.of(2026, 11, 9)),
            albumPhoto("other-vehicle", LocalDate.of(2026, 10, 12), vehicleId = "vehicle-2"),
        ).forEach { dao.insert(it) }

        val ids = dao.observeBetween("vehicle-1", LocalDate.of(2026, 9, 28), LocalDate.of(2026, 11, 8)).first()
            .map { it.id }

        assertThat(ids).containsExactly("start", "middle-early", "middle-late", "end").inOrder()
    }

    @Test
    fun albumUpdate_replacesFileAndMedia_keepingDateAndNote() = runTest {
        database.vehicleDao().upsert(vehicle())
        val dao = database.albumDao()
        val original = albumPhoto("p1", LocalDate.of(2026, 10, 1)).copy(comment = "Road trip")
        dao.insert(original)

        dao.update(
            original.copy(
                fileName = "album_new.mp4",
                thumbnailName = "album_new_thumb.jpg",
                mediaType = AlbumPhotoEntity.MEDIA_VIDEO,
                durationMs = 42_000,
                width = 1080,
                height = 1920,
            ),
        )

        val saved = dao.find("vehicle-1", "p1")!!
        assertThat(saved.fileName).isEqualTo("album_new.mp4")
        assertThat(saved.mediaType).isEqualTo(AlbumPhotoEntity.MEDIA_VIDEO)
        assertThat(saved.durationMs).isEqualTo(42_000)
        assertThat(saved.takenOn).isEqualTo(LocalDate.of(2026, 10, 1))
        assertThat(saved.comment).isEqualTo("Road trip")
        assertThat(albumPhoto("p2", LocalDate.of(2026, 10, 1)).mediaType).isEqualTo(AlbumPhotoEntity.MEDIA_PHOTO)
    }

    @Test
    fun attachmentObserveBetween_placesAttachmentsOnTheirRecordDate() = runTest {
        database.vehicleDao().upsert(vehicle())
        database.maintenanceDao().insertRecord(service("oil", "EngineOil", LocalDate.of(2026, 10, 8)))
        database.maintenanceDao().insertRecord(service("undated", "Tire", on = null))
        database.historyDao().insertFuel(fuel("fuel", LocalDate.of(2026, 10, 12)))
        database.historyDao().insertFuel(fuel("old-fuel", LocalDate.of(2026, 8, 1)))
        database.historyDao().insertCheck(check("repair", "Repair", LocalDate.of(2026, 9, 28), "Bumper"))
        val dao = database.attachmentDao()
        listOf(
            attachment("receipt-late", "Fuel", "fuel", createdAt = FIXED_NOW.plusSeconds(60)),
            attachment("receipt-early", "Fuel", "fuel"),
            attachment("oil-photo", "Maintenance", "oil"),
            attachment("repair-photo", "Repair", "repair"),
            attachment("undated-photo", "Maintenance", "undated"),
            attachment("old-receipt", "Fuel", "old-fuel"),
            // 종류가 다르면 같은 ID라도 다른 기록이다.
            attachment("wrong-type", "Note", "fuel"),
            // 녹음한 소리는 달력에 사진으로 놓지 않는다.
            sound("repair-sound", "Repair", "repair"),
        ).forEach { dao.insert(it) }

        val rows = dao.observeBetween("vehicle-1", LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 31)).first()

        assertThat(rows.map { it.attachment.id })
            .containsExactly("repair-photo", "oil-photo", "receipt-early", "receipt-late").inOrder()
        assertThat(rows.map { it.occurredOn }).containsExactly(
            LocalDate.of(2026, 9, 28),
            LocalDate.of(2026, 10, 8),
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 10, 12),
        ).inOrder()
    }

    @Test
    fun sounds_countSeparatelyFromPhotos_andAreLeftOutOfTimelinePhotoCount() = runTest {
        database.vehicleDao().upsert(vehicle())
        database.historyDao().insertCheck(check("repair", "Repair", LocalDate.of(2026, 9, 28), "Brake squeal"))
        val dao = database.attachmentDao()
        dao.insert(attachment("photo", "Repair", "repair"))
        dao.insert(sound("sound-1", "Repair", "repair"))
        dao.insert(sound("sound-2", "Repair", "repair"))

        assertThat(dao.count("vehicle-1", "Repair", "repair", AttachmentEntity.MEDIA_PHOTO)).isEqualTo(1)
        assertThat(dao.count("vehicle-1", "Repair", "repair", AttachmentEntity.MEDIA_AUDIO)).isEqualTo(2)
        val row = database.historyDao().observeTimeline("vehicle-1", listOf("Repair"), "", emptyList()).first().single()
        assertThat(row.attachmentCount).isEqualTo(1)

        dao.updateLabel("vehicle-1", "sound-1", "Brake squeal")
        assertThat(dao.find("vehicle-1", "sound-1")?.label).isEqualTo("Brake squeal")
        assertThat(dao.find("vehicle-1", "sound-2")?.label).isNull()
    }

    @Test
    fun deletingVehicle_cascadesToRecords() = runTest {
        database.vehicleDao().upsert(vehicle())
        database.mileageRecordDao().insert(mileage("a", 41_000, LocalDate.of(2026, 9, 1)))
        database.maintenanceDao().insertRecord(service("oil", "EngineOil", LocalDate.of(2026, 3, 10)))

        database.vehicleDao().delete("vehicle-1")

        assertThat(database.mileageRecordDao().observeLatest("vehicle-1").first()).isNull()
        assertThat(database.maintenanceDao().observeLatestRecords("vehicle-1").first()).isEmpty()
    }
}
