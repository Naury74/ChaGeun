package com.naury.chageun.data.maintenance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.RecordSourceTypes
import com.naury.chageun.core.database.entity.ReminderStateEntity
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.domain.reminder.MaintenanceReminderStage
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.ServiceRecord
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
class OfflineFirstMaintenanceRepositoryTest {

    private lateinit var database: ChageunDatabase
    private lateinit var repository: OfflineFirstMaintenanceRepository
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val vehicleId = VehicleId("v1")

    @Before
    fun setUp() = runTest {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChageunDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = OfflineFirstMaintenanceRepository(database, Clock.fixed(now, ZoneOffset.UTC))
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

    private fun rule(item: String) = MaintenanceRuleEntity(
        id = "rule-$item", vehicleId = "v1", itemType = item, intervalKm = 10_000, intervalMonths = 12,
        dueSoonKm = 500, dueSoonDays = 14, upcomingKm = 2_000, upcomingDays = 60, ruleSource = "Generic",
        sourceTitle = null, sourceUrl = null, isEnabled = true,
    )

    private fun record(id: String, item: String, date: LocalDate?, km: Long?) = MaintenanceRecordEntity(
        id = id, vehicleId = "v1", itemType = item, serviceDate = date, mileageKm = km, costWon = null,
        shopName = null, memo = null, sourceType = "USER", createdAt = now, updatedAt = now,
    )

    @Test
    fun mapsRulesRecordsAndMileage() = runTest {
        database.maintenanceDao().upsertRules(listOf(rule("EngineOil"), rule("Tire")))
        database.maintenanceDao().insertRecord(record("r1", "EngineOil", LocalDate.of(2026, 3, 10), 40_260))
        database.mileageRecordDao().insert(
            MileageRecordEntity("m1", "v1", 42_180, LocalDate.of(2026, 10, 1), "USER", null, now),
        )

        val inputs = repository.observeInputs(vehicleId).first()

        assertThat(inputs.rules.map { it.item }).containsExactly(MaintenanceItem.EngineOil, MaintenanceItem.Tire)
        assertThat(inputs.lastServices).containsExactly(
            MaintenanceItem.EngineOil,
            ServiceRecord(LocalDate.of(2026, 3, 10), Kilometers(40_260)),
        )
        assertThat(inputs.mileageHistory.single().mileage).isEqualTo(Kilometers(42_180))
    }

    @Test
    fun marksEstimatedMileage_inInputsAndHistory() = runTest {
        database.maintenanceDao().upsertRules(listOf(rule("Tire")))
        database.maintenanceDao().insertRecord(
            record("r1", "Tire", LocalDate.of(2026, 4, 1), 36_542).copy(sourceType = RecordSourceTypes.ESTIMATED),
        )

        val inputs = repository.observeInputs(vehicleId).first()
        val history = repository.observeServiceHistory(vehicleId, MaintenanceItem.Tire).first()

        assertThat(inputs.lastServices.getValue(MaintenanceItem.Tire).isMileageEstimated).isTrue()
        assertThat(history.single().isMileageEstimated).isTrue()
    }

    @Test
    fun skipsRowsWithUnknownItemNames() = runTest {
        database.maintenanceDao().upsertRules(listOf(rule("EngineOil"), rule("FluxCapacitor")))
        database.maintenanceDao().insertRecord(record("r1", "FluxCapacitor", LocalDate.of(2026, 1, 1), null))

        val inputs = repository.observeInputs(vehicleId).first()

        assertThat(inputs.rules.map { it.item }).containsExactly(MaintenanceItem.EngineOil)
        assertThat(inputs.lastServices).isEmpty()
    }

    @Test
    fun recordService_storesRecordAndOdometerReadingTogether() = runTest {
        repository.recordService(
            vehicleId,
            ServiceEntry(
                MaintenanceItem.EngineOil,
                LocalDate.of(2026, 9, 30),
                Kilometers(43_000),
                costWon = 0,
                shopName = " ",
            ),
            advancesOdometer = true,
        )

        val record = database.maintenanceDao().findLatestRecord("v1", "EngineOil")
        val mileage = database.mileageRecordDao().findLatest("v1")
        assertThat(record?.costWon).isEqualTo(0)
        assertThat(record?.shopName).isNull()
        assertThat(mileage?.mileageKm).isEqualTo(43_000)
        assertThat(mileage?.sourceType).isEqualTo("MAINTENANCE")
        assertThat(mileage?.relatedRecordId).isEqualTo(record?.id)
    }

    @Test
    fun recordService_leavesOdometerAlone_whenNotAdvancing() = runTest {
        repository.recordService(
            vehicleId,
            ServiceEntry(MaintenanceItem.Wiper, LocalDate.of(2026, 1, 5), Kilometers(30_000)),
            advancesOdometer = false,
        )

        assertThat(database.mileageRecordDao().findLatest("v1")).isNull()
    }

    @Test
    fun saveRule_updatesExistingRowAndKeepsDisabledRulesObservable() = runTest {
        database.maintenanceDao().upsertRules(listOf(rule("EngineOil")))
        val edited = repository.findRule(vehicleId, MaintenanceItem.EngineOil)!!
            .copy(intervalKm = 7_000, isEnabled = false, source = RuleSource.User)

        repository.saveRule(vehicleId, edited)

        val rules = repository.observeInputs(vehicleId).first().rules
        assertThat(rules).containsExactly(edited)
        assertThat(database.maintenanceDao().findRule("v1", "EngineOil")?.id).isEqualTo("rule-EngineOil")
    }

    @Test
    fun reminderStates_replaceAndReadBack() = runTest {
        val reminders = RoomReminderRepository(database.reminderDao(), Clock.fixed(now, ZoneOffset.UTC))

        reminders.replaceNotifiedStages(vehicleId, mapOf(MaintenanceItem.EngineOil to MaintenanceReminderStage.Due))
        reminders.replaceNotifiedStages(vehicleId, mapOf(MaintenanceItem.Tire to MaintenanceReminderStage.Near))

        assertThat(
            reminders.notifiedStages(vehicleId),
        ).containsExactly(MaintenanceItem.Tire, MaintenanceReminderStage.Near)
    }

    @Test
    fun reminderStates_savedBeforeStages_areReadAsTheClosestStage() = runTest {
        val reminders = RoomReminderRepository(database.reminderDao(), Clock.fixed(now, ZoneOffset.UTC))
        database.reminderDao().replaceAll(
            "v1",
            listOf("EngineOil" to "Upcoming", "Tire" to "Overdue", "Wiper" to "Somewhere").map { (item, state) ->
                ReminderStateEntity("v1", item, state, now)
            },
        )

        assertThat(reminders.notifiedStages(vehicleId)).containsExactly(
            MaintenanceItem.EngineOil,
            MaintenanceReminderStage.Early,
            MaintenanceItem.Tire,
            MaintenanceReminderStage.Due,
        )
    }

    @Test
    fun emitsAgain_whenServiceIsRecorded() = runTest {
        database.maintenanceDao().upsertRules(listOf(rule("EngineOil")))
        assertThat(repository.observeInputs(vehicleId).first().lastServices).isEmpty()

        database.maintenanceDao().insertRecord(record("r1", "EngineOil", LocalDate.of(2026, 9, 1), 42_000))

        assertThat(repository.observeInputs(vehicleId).first().lastServices).hasSize(1)
    }
}
