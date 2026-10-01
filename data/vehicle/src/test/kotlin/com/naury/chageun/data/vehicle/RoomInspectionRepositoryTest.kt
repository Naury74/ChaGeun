package com.naury.chageun.data.vehicle

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.domain.reminder.InspectionReminderStage
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
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
    fun clearingDate_removesSchedule() = runTest {
        repository.setUserDueDate(vehicleId, dueDate)

        repository.setUserDueDate(vehicleId, null)

        assertThat(repository.observeSchedule(vehicleId).first()).isNull()
    }
}
