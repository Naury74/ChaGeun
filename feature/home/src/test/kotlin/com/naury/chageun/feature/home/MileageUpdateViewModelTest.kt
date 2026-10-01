package com.naury.chageun.feature.home

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.UpdateMileageUseCase
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MileageUpdateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository()
    private val maintenance = FakeMaintenanceRepository()
    private val viewModel = MileageUpdateViewModel(
        SavedStateHandle(),
        vehicles,
        UpdateMileageUseCase(maintenance, Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)),
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(42_000)))
        maintenance.inputs.value =
            MaintenanceInputs(emptyList(), emptyMap(), listOf(MileageReading(TODAY.minusDays(40), Kilometers(42_891))))
    }

    @Test
    fun requiresValue() {
        viewModel.save()

        assertThat(viewModel.uiState.value.isMissing).isTrue()
    }

    @Test
    fun savesHigherValue() {
        viewModel.onMileageChanged("43,500")
        viewModel.save()

        assertThat(viewModel.uiState.value.isSaved).isTrue()
        assertThat(maintenance.inputs.value.mileageHistory.last().mileage).isEqualTo(Kilometers(43_500))
    }

    @Test
    fun warnsAboutLowerValue_andSavesCorrectionOnConfirm() {
        viewModel.onMileageChanged("1200")
        viewModel.save()

        assertThat(viewModel.uiState.value.lowerThan).isEqualTo(Kilometers(42_891))

        viewModel.confirmCorrection()

        assertThat(viewModel.uiState.value.isSaved).isTrue()
        assertThat(maintenance.mileageCorrections.single().mileage).isEqualTo(Kilometers(1_200))
    }
}
