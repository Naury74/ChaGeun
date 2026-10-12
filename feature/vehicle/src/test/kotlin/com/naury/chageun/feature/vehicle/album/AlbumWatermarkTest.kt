package com.naury.chageun.feature.vehicle.album

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import java.util.Locale
import org.junit.Test

class AlbumWatermarkTest {

    private val vehicle = Vehicle(
        id = VehicleId("v1"),
        maker = "Kia",
        model = "Sportage",
        modelYear = 2023,
        trim = "Signature",
        fuelType = FuelType.Gasoline,
        firstRegistrationDate = null,
        displacementCc = 1_598,
        plateMasked = "12가 **56",
        registrationMode = RegistrationMode.Manual,
        isPrimary = true,
    )
    private val labels = WatermarkLabels("차근 정비 기록") { String.format(Locale.US, "%,dkm", it.value) }
    private val log = listOf(
        entry(LocalDate.of(2026, 11, 1), 61_500),
        entry(LocalDate.of(2026, 10, 12), 60_000),
        entry(LocalDate.of(2026, 10, 12), 59_900),
        entry(LocalDate.of(2026, 9, 1), 57_000),
    )

    @Test
    fun suggestions_useCarDateAndMileage_inThatOrder() {
        val watermark = albumWatermark(vehicle, LocalDate.of(2026, 10, 12), log, labels)

        assertThat(watermark.text).isEqualTo("Kia Sportage · 2026.10.12")
        assertThat(watermark.suggestions).containsExactly(
            "Kia Sportage · 2026.10.12",
            "2026.10.12 · 60,000km",
            "차근 정비 기록",
            "2026.10.12",
        ).inOrder()
    }

    @Test
    fun suggestions_neverIncludePlateOrTrimDetails() {
        val watermark = albumWatermark(vehicle, LocalDate.of(2026, 10, 12), log, labels)

        (watermark.suggestions + watermark.text).forEach { text ->
            assertThat(text).doesNotContain("12가")
            assertThat(text).doesNotContain("**56")
            assertThat(text).doesNotContain(vehicle.plateMasked!!)
        }
    }

    @Test
    fun mileage_usesClosestReadingOnOrBeforeThePhotoDay() {
        assertThat(mileageOn(log, LocalDate.of(2026, 10, 20))).isEqualTo(Kilometers(60_000))
        assertThat(mileageOn(log, LocalDate.of(2026, 9, 1))).isEqualTo(Kilometers(57_000))
    }

    @Test
    fun mileage_beforeAnyReading_usesTheLatest() {
        assertThat(mileageOn(log, LocalDate.of(2026, 1, 1))).isEqualTo(Kilometers(61_500))
        assertThat(mileageOn(emptyList(), LocalDate.of(2026, 1, 1))).isNull()
    }

    @Test
    fun withoutVehicleOrMileage_startsWithAppName() {
        val watermark = albumWatermark(null, LocalDate.of(2026, 10, 12), emptyList(), labels)

        assertThat(watermark.text).isEqualTo("차근 정비 기록")
        assertThat(watermark.suggestions).containsExactly("차근 정비 기록", "2026.10.12").inOrder()
    }

    private fun entry(date: LocalDate, km: Long) = MileageEntry("m$km", date, Kilometers(km), MileageSource.User)
}
