package com.naury.chageun.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.RecordSourceTypes
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstHistoryRepositoryTest {

    private lateinit var database: ChageunDatabase
    private lateinit var repository: OfflineFirstHistoryRepository
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val vehicleId = VehicleId("v1")

    @Before
    fun setUp() = runTest {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChageunDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = OfflineFirstHistoryRepository(database, Clock.fixed(now, ZoneOffset.UTC))
        database.vehicleDao().upsert(
            VehicleEntity(
                id = "v1", plateNumberEncrypted = null, plateMasked = null, maker = "Maker", model = "Model",
                modelYear = 2023, trim = null, fuelType = "Gasoline", firstRegistrationDate = null, vinEncrypted = null,
                registrationMode = "Manual", isPrimary = true, createdAt = now, updatedAt = now,
            ),
        )
    }

    @After
    fun tearDown() = database.close()

    private val fuel = FuelEntry(
        date = LocalDate.of(2026, 8, 3),
        mileage = Kilometers(43_000),
        amounts = FuelAmounts(70_000, 41_176, 1_700, FuelField.Volume),
        isFullTank = true,
        stationName = " S-Oil ",
    )

    @Test
    fun addFuel_roundTripsThroughTimelineAndDetail_withOdometer() = runTest {
        repository.addFuel(vehicleId, fuel, advancesOdometer = true)

        val item = repository.observeTimeline(vehicleId, TimelineQuery()).first().single()
        val detail = repository.observeRecord(vehicleId, item.ref).first() as RecordDetail.Fuel
        assertThat(item.ref.type).isEqualTo(TimelineEventType.Fuel)
        assertThat(item.title).isEqualTo("S-Oil")
        assertThat(item.costWon).isEqualTo(70_000)
        assertThat(detail.entry).isEqualTo(fuel.copy(stationName = "S-Oil"))
        assertThat(database.mileageRecordDao().findLatest("v1")?.sourceType).isEqualTo("FUEL")
    }

    @Test
    fun delete_removesRecordAndItsOdometerReading() = runTest {
        repository.addCheck(
            vehicleId,
            CheckEntry(CheckKind.Repair, LocalDate.of(2026, 9, 1), "Bumper", Kilometers(44_000)),
            true,
        )
        val ref = repository.observeTimeline(vehicleId, TimelineQuery()).first().single().ref

        repository.delete(vehicleId, ref)

        assertThat(repository.observeTimeline(vehicleId, TimelineQuery()).first()).isEmpty()
        assertThat(database.mileageRecordDao().findLatest("v1")).isNull()
    }

    @Test
    fun filtersByTypeAndKeyword() = runTest {
        repository.addFuel(vehicleId, fuel, advancesOdometer = false)
        repository.addCheck(vehicleId, CheckEntry(CheckKind.Note, LocalDate.of(2026, 9, 1), "Car wash"), false)

        val notes = repository.observeTimeline(vehicleId, TimelineQuery(types = setOf(TimelineEventType.Note))).first()
        val wash = repository.observeTimeline(vehicleId, TimelineQuery(keyword = "wash")).first()

        assertThat(notes.map { it.title }).containsExactly("Car wash")
        assertThat(wash.map { it.title }).containsExactly("Car wash")
    }

    @Test
    fun marksEstimatedMileage_inTimelineAndDetail() = runTest {
        val now = Instant.parse("2026-10-01T00:00:00Z")
        database.maintenanceDao().insertRecord(
            MaintenanceRecordEntity(
                "r1", "v1", "Tire", LocalDate.of(2026, 4, 1), 36_542, null, null, null,
                RecordSourceTypes.ESTIMATED, now, now,
            ),
        )

        val item = repository.observeTimeline(vehicleId, TimelineQuery()).first().single()
        val detail = repository.observeRecord(vehicleId, item.ref).first() as RecordDetail.Maintenance
        assertThat(item.isMileageEstimated).isTrue()
        assertThat(detail.entry.isMileageEstimated).isTrue()
    }

    @Test
    fun updateFuel_keepsIdAndCreatedAt_andMovesOdometer() = runTest {
        repository.addFuel(vehicleId, fuel, advancesOdometer = true)
        val before = repository.observeTimeline(vehicleId, TimelineQuery()).first().single()

        repository.updateFuel(vehicleId, before.ref.id, fuel.copy(mileage = Kilometers(43_500), stationName = "GS"))

        val after = repository.observeTimeline(vehicleId, TimelineQuery()).first().single()
        assertThat(after.ref).isEqualTo(before.ref)
        assertThat(after.createdAt).isEqualTo(before.createdAt)
        assertThat(after.title).isEqualTo("GS")
        assertThat(database.mileageRecordDao().findLatest("v1")?.mileageKm).isEqualTo(43_500)
    }

    @Test
    fun updateCheck_dropsOdometer_whenEditedBelowLatestReading() = runTest {
        database.mileageRecordDao().insert(
            MileageRecordEntity("m0", "v1", 44_500, LocalDate.of(2026, 8, 1), "USER", null, now),
        )
        repository.addCheck(
            vehicleId,
            CheckEntry(CheckKind.Repair, LocalDate.of(2026, 9, 1), "Bumper", Kilometers(45_000)),
            true,
        )
        val ref = repository.observeTimeline(vehicleId, TimelineQuery()).first().single().ref

        repository.updateCheck(
            vehicleId,
            ref.id,
            CheckEntry(CheckKind.Repair, LocalDate.of(2026, 9, 1), "Bumper", Kilometers(44_000)),
        )

        assertThat(database.mileageRecordDao().findLatest("v1")?.id).isEqualTo("m0")
    }

    @Test
    fun updateMaintenance_clearsEstimatedMark() = runTest {
        database.maintenanceDao().insertRecord(
            MaintenanceRecordEntity(
                "r1", "v1", "Tire", LocalDate.of(2026, 4, 1), 36_542, null, null, null,
                RecordSourceTypes.ESTIMATED, now, now,
            ),
        )

        repository.updateMaintenance(
            vehicleId,
            "r1",
            ServiceEntry(MaintenanceItem.Tire, LocalDate.of(2026, 4, 2), Kilometers(36_000), costWon = 320_000),
        )

        val ref = RecordRef(TimelineEventType.Maintenance, "r1")
        val detail = repository.observeRecord(vehicleId, ref).first() as RecordDetail.Maintenance
        assertThat(detail.entry.isMileageEstimated).isFalse()
        assertThat(detail.entry.costWon).isEqualTo(320_000)
        assertThat(detail.item).isEqualTo(MaintenanceItem.Tire)
    }

    @Test
    fun filtersByPeriodCostAndAttachments_andCountsAttachments() = runTest {
        repository.addCheck(
            vehicleId,
            CheckEntry(CheckKind.Repair, LocalDate.of(2026, 9, 1), "Bumper", costWon = 120_000),
            false,
        )
        repository.addCheck(
            vehicleId,
            CheckEntry(CheckKind.Note, LocalDate.of(2026, 3, 1), "Wash", costWon = 10_000),
            false,
        )
        repository.addCheck(vehicleId, CheckEntry(CheckKind.Note, LocalDate.of(2026, 9, 5), "No cost"), false)
        val bumper = repository.observeTimeline(vehicleId, TimelineQuery(keyword = "Bumper")).first().single()
        database.attachmentDao().insert(
            AttachmentEntity(
                "a1", "v1", bumper.ref.type.name, bumper.ref.id, "a1.jpg", "a1_t.jpg", "image/jpeg", 10, now,
            ),
        )

        fun titles(query: TimelineQuery) = repository.observeTimeline(vehicleId, query).map { rows ->
            rows.map { it.title }
        }

        assertThat(titles(TimelineQuery(dateFrom = LocalDate.of(2026, 8, 1))).first())
            .containsExactly("No cost", "Bumper").inOrder()
        assertThat(titles(TimelineQuery(minCostWon = 50_000)).first()).containsExactly("Bumper")
        assertThat(titles(TimelineQuery(maxCostWon = 50_000)).first()).containsExactly("Wash")
        assertThat(titles(TimelineQuery(withAttachmentsOnly = true)).first()).containsExactly("Bumper")
        assertThat(
            repository.observeTimeline(vehicleId, TimelineQuery(keyword = "Bumper")).first().single().attachmentCount,
        )
            .isEqualTo(1)
    }

    @Test
    fun limit_readsNewestFirst_andMonthlyCostsCoverEveryRecord() = runTest {
        // 9월에 기록 3개, 8월에 1개, 날짜 없는 비용 기록은 없음. 두 개만 읽어도 9월 합계는 세 개 모두다.
        listOf(
            LocalDate.of(2026, 9, 20) to 30_000L,
            LocalDate.of(2026, 9, 10) to 20_000L,
            LocalDate.of(2026, 9, 1) to 10_000L,
            LocalDate.of(2026, 8, 15) to 5_000L,
        ).forEachIndexed { index, (date, cost) ->
            repository.addCheck(vehicleId, CheckEntry(CheckKind.Repair, date, "R$index", costWon = cost), false)
        }

        val firstPage = repository.observeTimeline(vehicleId, TimelineQuery(), limit = 2).first()
        val totals = repository.observeMonthlyCosts(vehicleId, TimelineQuery()).first()

        assertThat(firstPage.map { it.title }).containsExactly("R0", "R1").inOrder()
        assertThat(totals).containsExactly(YearMonth.of(2026, 9), 60_000L, YearMonth.of(2026, 8), 5_000L)
    }

    @Test
    fun monthlyCosts_followTheSameFiltersAsTheTimeline() = runTest {
        repository.addFuel(vehicleId, fuel, advancesOdometer = false)
        repository.addCheck(
            vehicleId,
            CheckEntry(CheckKind.Repair, LocalDate.of(2026, 8, 20), "Bumper", costWon = 120_000),
            false,
        )
        val bumper = repository.observeTimeline(vehicleId, TimelineQuery(keyword = "Bumper")).first().single()
        database.attachmentDao().insert(
            AttachmentEntity(
                "a1", "v1", bumper.ref.type.name, bumper.ref.id, "a1.jpg", "a1_t.jpg", "image/jpeg", 10, now,
            ),
        )
        val august = YearMonth.of(2026, 8)

        fun total(query: TimelineQuery) = repository.observeMonthlyCosts(vehicleId, query).map { it[august] }

        assertThat(total(TimelineQuery()).first()).isEqualTo(190_000L)
        assertThat(total(TimelineQuery(types = setOf(TimelineEventType.Fuel))).first()).isEqualTo(70_000L)
        assertThat(total(TimelineQuery(keyword = "Bumper")).first()).isEqualTo(120_000L)
        assertThat(total(TimelineQuery(withAttachmentsOnly = true)).first()).isEqualTo(120_000L)
        assertThat(total(TimelineQuery(dateFrom = LocalDate.of(2026, 8, 10))).first()).isEqualTo(120_000L)
    }
}
