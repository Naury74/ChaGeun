package com.naury.chageun.feature.vehicle.album

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAlbumRepository
import com.naury.chageun.core.testing.FakeVehiclePhotoRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import com.naury.chageun.core.ui.photo.EditedPhotos
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

        recreated.delete(listOf(id))
        assertThat(recreated.uiState.first { it.photos.size == 1 }.selected).isNull()
    }

    @Test
    fun picking_togglesWholeDays_andDeletesTogether() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.add(listOf("content://a", "content://b", "content://c"), highQuality = false)
        val ids = viewModel.uiState.first { it.photos.size == 3 }.photos.map { it.id }

        viewModel.picks.toggle(ids[0])
        assertThat(viewModel.uiState.first { it.isPicking }.picked).containsExactly(ids[0])

        viewModel.pickDay(today)
        assertThat(viewModel.uiState.first { it.picked.size == 3 }.picked).containsExactlyElementsIn(ids)
        viewModel.pickDay(today)
        assertThat(viewModel.uiState.first { !it.isPicking }.picked).isEmpty()

        viewModel.picks.toggle(ids[1])
        viewModel.picks.toggle(ids[2])
        viewModel.select(ids[2])
        viewModel.delete(viewModel.uiState.first { it.picked.size == 2 }.picked.toList())

        val state = viewModel.uiState.first { it.photos.size == 1 }
        assertThat(state.photos.single().id).isEqualTo(ids[0])
        assertThat(state.picked).isEmpty()
        assertThat(state.selected).isNull()
    }

    @Test
    fun gridZoom_staysWithinSteps_andSurvivesRecreation() = runTest {
        val handle = SavedStateHandle()
        val viewModel = viewModel(handle)
        backgroundScope.launch { viewModel.uiState.collect {} }

        repeat(GRID_STEP_COUNT + 2) { viewModel.zoomGrid(1) }
        assertThat(viewModel.uiState.first { it.gridStep == GRID_STEP_COUNT - 1 }.gridStep).isEqualTo(
            GRID_STEP_COUNT - 1,
        )
        repeat(GRID_STEP_COUNT + 2) { viewModel.zoomGrid(-1) }

        val recreated = viewModel(handle)
        backgroundScope.launch { recreated.uiState.collect {} }
        assertThat(recreated.uiState.first { !it.isLoading }.gridStep).isEqualTo(0)
    }

    @Test
    fun videos_areAdded_andTooLargeOnesAreReportedSeparately() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.add(listOf("content://video/1", "content://huge-video/2", "content://broken"), highQuality = false)

        val state = viewModel.uiState.first { it.photos.size == 1 }
        assertThat(state.photos.single().isVideo).isTrue()
        assertThat(state.tooLargeCount).isEqualTo(1)
        assertThat(state.failedCount).isEqualTo(1)
        viewModel.dismissMessage()
        assertThat(viewModel.uiState.first { it.tooLargeCount == 0 }.failedCount).isEqualTo(0)
    }

    @Test
    fun editTag_add_putsEditedMediaInTheAlbum() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        AlbumEditTag.apply(EditedPhotos(listOf("file:///cache/edited.mp4"), AlbumEditTag.ADD), viewModel)

        val state = viewModel.uiState.first { it.editing != null }
        assertThat(state.photos.single().isVideo).isTrue()
    }

    @Test
    fun editTag_replace_swapsTheFileInPlace_keepingDateAndNote() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.add(listOf("content://a", "content://b"), highQuality = false)
        val target = viewModel.uiState.first { it.photos.size == 2 }.photos.last()
        viewModel.saveDetails(target.id, today.minusDays(1), "Road trip")
        viewModel.uiState.first { state -> state.photos.any { it.comment == "Road trip" } }

        AlbumEditTag.apply(EditedPhotos(listOf("file:///cache/edited.jpg"), AlbumEditTag.replace(target.id)), viewModel)

        val replaced = viewModel.uiState.first { state -> state.photos.any { it.filePath.endsWith("edited.jpg") } }
            .photos.single { it.id == target.id }
        assertThat(replaced.comment).isEqualTo("Road trip")
        assertThat(replaced.takenOn).isEqualTo(today.minusDays(1))
        assertThat(album.replaced).containsExactly(target.id to "file:///cache/edited.jpg")
    }

    @Test
    fun replaceFailure_showsNotice_untilDismissed() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.add(listOf("content://a", "content://b"), highQuality = false)
        val original = viewModel.uiState.first { it.photos.size == 2 }.photos.first()

        viewModel.replaceMedia(original.id, "file:///cache/broken.jpg")

        val failed = viewModel.uiState.first { it.isEditFailed }
        assertThat(failed.photos.single { it.id == original.id }.filePath).isEqualTo(original.filePath)
        viewModel.dismissMessage()
        assertThat(viewModel.uiState.first { !it.isEditFailed }.isEditFailed).isFalse()
    }

    @Test
    fun editTag_withoutKnownTag_isIgnored() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        AlbumEditTag.apply(EditedPhotos(listOf("file:///cache/edited.jpg"), tag = null), viewModel)
        AlbumEditTag.apply(EditedPhotos(listOf("file:///cache/edited.jpg"), tag = "replace:a1"), viewModel)

        assertThat(viewModel.uiState.first { !it.isLoading }.photos).isEmpty()
        assertThat(album.replaced).isEmpty()
    }

    @Test
    fun editTag_slideshow_addsAVideoOnTheLatestPickedDay_andAsksForANote() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val day = LocalDate.of(2026, 9, 21)

        AlbumEditTag.apply(EditedPhotos(listOf("file:///cache/slideshow.mp4"), AlbumEditTag.slideshow(day)), viewModel)

        val state = viewModel.uiState.first { it.editing != null }
        assertThat(state.editing!!.isVideo).isTrue()
        assertThat(state.editing!!.takenOn).isEqualTo(day)
        assertThat(state.days.single().date).isEqualTo(day)
    }

    @Test
    fun slideshow_withUnreadableDate_fallsBackToToday() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        AlbumEditTag.apply(EditedPhotos(listOf("file:///cache/slideshow.mp4"), "album-slideshow:broken"), viewModel)

        assertThat(viewModel.uiState.first { it.editing != null }.editing!!.takenOn).isEqualTo(today)
    }

    @Test
    fun slideshowFromSelection_keepsPickOrder_skipsVideos_andUsesLatestDate() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.add(listOf("content://a", "content://b", "content://video"), highQuality = false)
        val added = viewModel.uiState.first { it.photos.size == 3 }.photos
        val (a, b) = added.filterNot { it.isVideo }
        val video = added.single { it.isVideo }
        viewModel.saveDetails(a.id, today.minusDays(10), "")
        viewModel.uiState.first { state -> state.photos.any { it.takenOn == today.minusDays(10) } }
        listOf(b.id, video.id, a.id).forEach(viewModel.picks::toggle)

        val state = viewModel.uiState.first { it.picked.size == 3 }

        assertThat(state.slideshowPhotos().map { it.id }).containsExactly(b.id, a.id).inOrder()
        assertThat(state.hasPickedVideos).isTrue()
        assertThat(slideshowDate(state.slideshowPhotos(), today)).isEqualTo(today)
    }

    @Test
    fun uiState_carriesVehicleAndMileage_forWatermarkSuggestions() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val entry = MileageEntry("m1", today, Kilometers(41_000), MileageSource.User)
        vehicles.mileageLog.value = listOf(entry)

        val state = viewModel.uiState.first { it.mileageLog.isNotEmpty() }

        assertThat(state.vehicle?.model).isEqualTo("Model")
        assertThat(state.mileageLog).containsExactly(entry)
    }
}
