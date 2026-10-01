package com.naury.chageun.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
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
}
