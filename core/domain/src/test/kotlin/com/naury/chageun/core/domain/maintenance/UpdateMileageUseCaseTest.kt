package com.naury.chageun.core.domain.maintenance

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UpdateMileageUseCaseTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val vehicleId = VehicleId("v1")
    private val repository = FakeMaintenanceRepository()
    private val updateMileage =
        UpdateMileageUseCase(repository, Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC))

    @Before
    fun setUp() {
        repository.inputs.value =
            MaintenanceInputs(
                emptyList(),
                emptyMap(),
                listOf(MileageReading(LocalDate.of(2026, 9, 1), Kilometers(42_891))),
            )
    }

    @Test
    fun savesHigherReading_forToday() = runTest {
        assertThat(updateMileage(vehicleId, Kilometers(43_500))).isEqualTo(UpdateMileageResult.Saved)
        assertThat(repository.inputs.value.mileageHistory.last()).isEqualTo(MileageReading(today, Kilometers(43_500)))
        assertThat(repository.mileageCorrections).isEmpty()
    }

    @Test
    fun asksBeforeSavingLowerReading_thenStoresCorrection() = runTest {
        assertThat(
            updateMileage(vehicleId, Kilometers(1_200)),
        ).isEqualTo(UpdateMileageResult.NeedsConfirmation(Kilometers(42_891)))
        assertThat(repository.inputs.value.mileageHistory).hasSize(1)

        updateMileage(vehicleId, Kilometers(1_200), isCorrectionConfirmed = true)

        assertThat(repository.mileageCorrections).containsExactly(MileageReading(today, Kilometers(1_200)))
    }
}
