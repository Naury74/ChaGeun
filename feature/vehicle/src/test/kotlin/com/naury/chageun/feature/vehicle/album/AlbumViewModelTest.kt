package com.naury.chageun.feature.vehicle.album

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAlbumRepository
import com.naury.chageun.core.testing.FakeVehiclePhotoRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AlbumViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository()
    private val album = FakeAlbumRepository()
    private val today = LocalDate.of(2026, 10, 6)

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = AlbumViewModel(
        handle,
        vehicles,
        album,
        FakeVehiclePhotoRepository(),
        Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneOffset.UTC),
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(40_000)))
    }

    @Test
    fun singlePhoto_opensDetailsInput_andGroupsByDay() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.add(listOf("content://one"), highQuality = false)

        val state = viewModel.uiState.first { it.editing != null }
        assertThat(state.days.single().date).isEqualTo(today)
        viewModel.saveDetails(state.editing!!.id, today.minusDays(3), "Car wash")
        val saved = viewModel.uiState.first { it.editing == null && it.photos.single().comment != null }
        assertThat(saved.days.single().date).isEqualTo(today.minusDays(3))
        assertThat(saved.photos.single().comment).isEqualTo("Car wash")
    }

    @Test
    fun manyPhotos_skipDetailsInput_andReportFailures() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.add(listOf("content://a", "content://b", "content://broken"), highQuality = false)

        val state = viewModel.uiState.first { it.photos.size == 2 }
        assertThat(state.editing).isNull()
        assertThat(state.failedCount).isEqualTo(1)
    }

    @Test
    fun selection_survivesRecreation_andClearsOnDelete() = runTest {
        val handle = SavedStateHandle()
        val viewModel = viewModel(handle)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.add(listOf("content://a", "content://b"), highQuality = false)
        val id = viewModel.uiState.first { it.photos.size == 2 }.photos.first().id

        viewModel.select(id)
        val recreated = viewModel(handle)
        backgroundScope.launch { recreated.uiState.collect {} }
        assertThat(recreated.uiState.first { it.selected != null }.selected?.id).isEqualTo(id)

        recreated.delete(id)
        assertThat(recreated.uiState.first { it.photos.size == 1 }.selected).isNull()
    }
}
