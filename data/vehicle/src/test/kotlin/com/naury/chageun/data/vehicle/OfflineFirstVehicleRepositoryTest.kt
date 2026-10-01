package com.naury.chageun.data.vehicle

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.PlateNumber
import com.naury.chageun.core.model.PlateParseResult
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.security.FieldCipher
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstVehicleRepositoryTest {

    private lateinit var database: ChageunDatabase
    private lateinit var repository: OfflineFirstVehicleRepository

    private val clock = Clock.fixed(Instant.parse("2026-10-01T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val reversingCipher = object : FieldCipher {
        override fun encrypt(plaintext: String) = "enc:" + plaintext.reversed()

        override fun decrypt(ciphertext: String) = ciphertext.removePrefix("enc:").reversed()
    }

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChageunDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = OfflineFirstVehicleRepository(database, reversingCipher, clock)
    }

    @After
    fun tearDown() = database.close()

    private fun registration(fuelType: FuelType = FuelType.Gasoline, plate: String? = "123가4567") = VehicleRegistration(
        maker = " Maker ",
        model = "Model",
        modelYear = 2023,
        fuelType = fuelType,
        currentMileage = Kilometers(42_180),
        plate = plate?.let { (PlateNumber.parse(it) as PlateParseResult.Valid).plate },
    )

    @Test
    fun registersPrimaryVehicle_withMaskedAndEncryptedPlate() = runTest {
        val id = repository.register(registration())

        val vehicle = repository.observePrimaryVehicle().first()
        val stored = database.vehicleDao().observe(id.value).first()
        assertThat(vehicle?.id).isEqualTo(id)
        assertThat(vehicle?.maker).isEqualTo("Maker")
        assertThat(vehicle?.plateMasked).isEqualTo("123가 **67")
        assertThat(stored?.plateNumberEncrypted).isEqualTo("enc:7654가321")
    }

    @Test
    fun storesStartingMileage_onToday() = runTest {
        val id = repository.register(registration())

        val latest = database.mileageRecordDao().observeLatest(id.value).first()
        assertThat(latest?.mileageKm).isEqualTo(42_180)
        assertThat(latest?.recordedOn).isEqualTo(LocalDate.of(2026, 10, 1))
    }

    @Test
    fun seedsGenericRules_forFuelType() = runTest {
        val gasoline = repository.register(registration(FuelType.Gasoline))
        val electric = repository.register(registration(FuelType.Electric, plate = null))

        val gasolineRules = database.maintenanceDao().observeEnabledRules(gasoline.value).first()
        val electricRules = database.maintenanceDao().observeEnabledRules(electric.value).first()
        assertThat(gasolineRules).hasSize(12)
        assertThat(electricRules.map { it.itemType }).containsNoneOf("EngineOil", "SparkPlug")
        assertThat(gasolineRules.map { it.ruleSource }.toSet()).containsExactly("Generic")
    }

    @Test
    fun newRegistration_becomesTheOnlyPrimaryVehicle() = runTest {
        val first = repository.register(registration())
        val second = repository.register(registration(plate = null))

        assertThat(repository.observePrimaryVehicle().first()?.id).isEqualTo(second)
        assertThat(database.vehicleDao().observe(first.value).first()?.isPrimary).isFalse()
    }
}
