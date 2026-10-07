package com.naury.chageun.data.vehicle

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.domain.reminder.InspectionReminderStage
import com.naury.chageun.core.model.InspectionRecord
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.PeriodicInspectionResult
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
class RoomInspectionRepositoryTest {

    private lateinit var database: ChageunDatabase
    private lateinit var repository: RoomInspectionRepository
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val vehicleId = VehicleId("v1")
    private val dueDate = LocalDate.of(2026, 10, 20)

    @Before
    fun setUp() = runTest {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChageunDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        database.vehicleDao().upsert(
            VehicleEntity(
                id = "v1", plateNumberEncrypted = null, plateMasked = null, maker = "Maker", model = "Model",
                modelYear = 2023, trim = null, fuelType = "Gasoline", firstRegistrationDate = null,
                vinEncrypted = null, registrationMode = "Manual", isPrimary = true, createdAt = now, updatedAt = now,
            ),
        )
        repository = RoomInspectionRepository(database.inspectionDao(), Clock.fixed(now, ZoneOffset.UTC))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun userDate_isObservedAsUserSource() = runTest {
        repository.setUserDueDate(vehicleId, dueDate)

        assertThat(repository.observeSchedule(vehicleId).first())
            .isEqualTo(InspectionSchedule(dueDate, InspectionSource.User))
    }

    @Test
    fun changingDate_restartsReminders_butSavingSameDateKeepsThem() = runTest {
        repository.setUserDueDate(vehicleId, dueDate)
        repository.markNotified(vehicleId, InspectionReminderStage.Days30)

        repository.setUserDueDate(vehicleId, dueDate)
        assertThat(repository.notifiedStage(vehicleId)).isEqualTo(InspectionReminderStage.Days30)

        repository.setUserDueDate(vehicleId, dueDate.plusYears(2))
        assertThat(repository.notifiedStage(vehicleId)).isNull()
    }

    @Test
    fun history_listsOnlyPeriodicInspections_newestFirst() = runTest {
        val history = database.historyDao()
        history.insertCheck(check("old", LocalDate.of(2024, 9, 1), mileageKm = 20_000, periodicResult = "Unknown"))
        history.insertCheck(check("free", LocalDate.of(2026, 5, 1), mileageKm = 38_000, periodicResult = null))
        history.insertCheck(check("new", LocalDate.of(2026, 9, 1), mileageKm = null, periodicResult = "Failed"))

        assertThat(repository.observeHistory(vehicleId).first()).containsExactly(
            InspectionRecord(LocalDate.of(2026, 9, 1), null, PeriodicInspectionResult.Failed),
            InspectionRecord(LocalDate.of(2024, 9, 1), Kilometers(20_000), PeriodicInspectionResult.Unknown),
        ).inOrder()
    }

    private fun check(id: String, date: LocalDate, mileageKm: Long?, periodicResult: String?) = CheckRecordEntity(
        id = id,
        vehicleId = "v1",
        kind = "Inspection",
        checkDate = date,
        title = id,
        mileageKm = mileageKm,
        costWon = null,
        memo = null,
        createdAt = now,
        updatedAt = now,
        periodicResult = periodicResult,
    )

    @Test
    fun clearingDate_removesSchedule() = runTest {
        repository.setUserDueDate(vehicleId, dueDate)

        repository.setUserDueDate(vehicleId, null)

        assertThat(repository.observeSchedule(vehicleId).first()).isNull()
    }
}
