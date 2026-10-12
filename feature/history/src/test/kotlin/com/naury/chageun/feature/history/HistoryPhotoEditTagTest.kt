package com.naury.chageun.feature.history

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.history.DeleteHistoryRecordUseCase
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAttachmentRepository
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import com.naury.chageun.core.ui.photo.EditedPhotos
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryPhotoEditTagTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository()
    private val history = FakeHistoryRepository()
    private val attachments = FakeAttachmentRepository()
    private val ref = RecordRef(TimelineEventType.Fuel, "fuel-1")
    private val existing =
        Attachment("a1", ref, "/files/attachments/a1.jpg", "/files/attachments/a1_thumb.jpg", Instant.EPOCH)

    private fun viewModel() = HistoryViewModel(
        SavedStateHandle(),
        vehicles,
        history,
        attachments,
        DeleteHistoryRecordUseCase(history, attachments),
        Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC),
    )

    @Before
    fun setUp() = runTest {
        vehicles.register(VehicleRegistration("Maker", "Model", 2023, FuelType.Gasoline, Kilometers(40_000)))
        attachments.attachments.value = listOf(existing)
    }

    @Test
    fun editedAttachment_replacesItInPlace() = runTest {
        val vm = viewModel()

        HistoryPhotoEditTag.apply(EditedPhotos(listOf("file:///cache/e.jpg"), HistoryPhotoEditTag.replace("a1")), vm)
        advanceUntilIdle()

        assertThat(attachments.attachments.value.single().id).isEqualTo("a1")
        assertThat(attachments.attachments.value.single().filePath).isEqualTo("file:///cache/e.jpg")
    }

    @Test
    fun pickedAndEdited_attachToTheRecord_inHighQuality() = runTest {
        val vm = viewModel()
        val uris = listOf("file:///cache/1.jpg", "file:///cache/2.jpg")

        HistoryPhotoEditTag.apply(EditedPhotos(uris, HistoryPhotoEditTag.add(ref)), vm)
        advanceUntilIdle()

        assertThat(attachments.attachments.value.map { it.filePath }).containsAtLeastElementsIn(uris)
        assertThat(attachments.attachments.value.all { it.owner == ref }).isTrue()
        assertThat(attachments.lastHighQuality).isTrue()
    }

    @Test
    fun failedReplace_showsAttachFailure() = runTest {
        attachments.replaceSucceeds = false
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        HistoryPhotoEditTag.apply(EditedPhotos(listOf("file:///cache/e.jpg"), HistoryPhotoEditTag.replace("a1")), vm)

        assertThat(vm.uiState.first { it.attachFailedCount > 0 }.attachFailedCount).isEqualTo(1)
        assertThat(attachments.attachments.value.single()).isEqualTo(existing)
    }

    @Test
    fun resultWithoutKnownTag_isIgnored() = runTest {
        val vm = viewModel()

        HistoryPhotoEditTag.apply(EditedPhotos(listOf("file:///cache/e.jpg")), vm)
        HistoryPhotoEditTag.apply(EditedPhotos(listOf("file:///cache/e.jpg"), "other"), vm)
        advanceUntilIdle()

        assertThat(attachments.attachments.value).containsExactly(existing)
    }
}
