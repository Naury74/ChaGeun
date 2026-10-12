package com.naury.chageun.core.ui.photo

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.ui.R
import com.naury.framekit.android.result.EditedMedia
import com.naury.framekit.android.result.EditorError
import com.naury.framekit.android.result.EditorErrorCode
import com.naury.framekit.android.result.FrameKitResult
import com.naury.framekit.core.model.MediaType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoEditResultHandlerTest {

    private val staged = mutableListOf<Uri>()
    private val deleted = mutableListOf<Uri>()
    private var stageFails = false
    private val handler = PhotoEditResultHandler(
        stage = { uri ->
            staged += uri
            if (stageFails) null else "file:///cache/${uri.lastPathSegment}"
        },
        deleteOutput = { deleted += it },
    )

    @Test
    fun success_stagesEveryImage_thenDeletesEveryOutput() {
        val first = media("a.jpg")
        val second = media("b.jpg")

        val outcome = handler.handle(FrameKitResult.Success(first, listOf(first, second)))

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Edited(listOf("file:///cache/a.jpg", "file:///cache/b.jpg")))
        assertThat(deleted).containsExactly(first.uri, second.uri).inOrder()
    }

    @Test
    fun success_skipsNonImages_butStillDeletesThem() {
        val photo = media("a.jpg")
        val pdf = media("doc.pdf", MediaType.DOCUMENT)

        val outcome = handler.handle(FrameKitResult.Success(photo, listOf(photo, pdf)))

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Edited(listOf("file:///cache/a.jpg")))
        assertThat(staged).containsExactly(photo.uri)
        assertThat(deleted).containsExactly(photo.uri, pdf.uri)
    }

    @Test
    fun stagingFailure_reportsGenericError_andLeavesNoOutputBehind() {
        stageFails = true
        val photo = media("a.jpg")

        val outcome = handler.handle(FrameKitResult.Success(photo))

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Failed(R.string.photo_edit_error_generic))
        assertThat(deleted).containsExactly(photo.uri)
    }

    @Test
    fun stagingCrash_stillDeletesOutput() {
        val crashing = PhotoEditResultHandler(stage = { error("disk") }, deleteOutput = { deleted += it })
        val photo = media("a.jpg")

        runCatching { crashing.handle(FrameKitResult.Success(photo)) }

        assertThat(deleted).containsExactly(photo.uri)
    }

    @Test
    fun cancelled_doesNothing() {
        assertThat(handler.handle(FrameKitResult.Cancelled)).isEqualTo(PhotoEditOutcome.Cancelled)
        assertThat(staged).isEmpty()
        assertThat(deleted).isEmpty()
    }

    @Test
    fun failure_mapsCodeToMessage() {
        val outcome = handler.handle(FrameKitResult.Failure(EditorError(EditorErrorCode.PERMISSION_DENIED)))

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Failed(R.string.photo_edit_error_unavailable))
        assertThat(deleted).isEmpty()
    }

    @Test
    fun errorCodes_groupIntoShortMessages() {
        val expected = mapOf(
            EditorErrorCode.PERMISSION_DENIED to R.string.photo_edit_error_unavailable,
            EditorErrorCode.SOURCE_UNAVAILABLE to R.string.photo_edit_error_unavailable,
            EditorErrorCode.INVALID_SOURCE to R.string.photo_edit_error_unsupported,
            EditorErrorCode.UNSUPPORTED_FORMAT to R.string.photo_edit_error_unsupported,
            EditorErrorCode.DECODE_FAILED to R.string.photo_edit_error_unsupported,
            EditorErrorCode.SOURCE_TOO_LARGE to R.string.photo_edit_error_too_large,
            EditorErrorCode.INSUFFICIENT_MEMORY to R.string.photo_edit_error_too_large,
            EditorErrorCode.INSUFFICIENT_STORAGE to R.string.photo_edit_error_storage,
        )
        EditorErrorCode.entries.forEach { code ->
            assertThat(code.messageRes()).isEqualTo(expected[code] ?: R.string.photo_edit_error_generic)
        }
    }

    private fun media(name: String, type: MediaType = MediaType.IMAGE) = EditedMedia(
        uri = Uri.parse("content://com.naury.chageun.framekit.files/exports/$name"),
        mediaType = type,
        width = 100,
        height = 100,
        durationMs = null,
        mimeType = if (type == MediaType.IMAGE) "image/jpeg" else "application/pdf",
        fileSize = 10,
    )
}
