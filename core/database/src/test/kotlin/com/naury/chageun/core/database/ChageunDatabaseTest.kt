package com.naury.chageun.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
    fun keepsZeroCost_distinctFromMissingCost() = runTest {
        database.vehicleDao().upsert(vehicle())
        val dao = database.maintenanceDao()
        dao.insertRecord(service("free", "Wiper", LocalDate.of(2026, 1, 1), costWon = 0))
        dao.insertRecord(service("unknown", "Battery", LocalDate.of(2026, 1, 1), costWon = null))

        val costs = dao.observeLatestRecords("vehicle-1").first().associate { it.id to it.costWon }

        assertThat(costs).containsExactly("free", 0L, "unknown", null)
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
