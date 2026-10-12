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
        stage = { uri, isVideo ->
            staged += uri
            if (stageFails) null else "file:///cache/${uri.lastPathSegment}" + if (isVideo) "#video" else ""
        },
        deleteOutput = { deleted += it },
        stageShare = { uri, extension ->
            shared += uri to extension
            if (stageFails) null else Uri.parse("content://com.naury.chageun.photos/photo_share/copy.$extension")
        },
    )
    private val shared = mutableListOf<Pair<Uri, String>>()

    @Test
    fun originals_areCopiedRightAway_andNeverDeleted() {
        val photo = original("content://media/picker/0/photo/1", MediaType.IMAGE, "image/heic")
        val video = original("content://media/picker/0/video/2", MediaType.VIDEO, "video/quicktime")

        val outcome = handler.handle(FrameKitResult.Success(photo, listOf(photo, video)))

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Edited(listOf("file:///cache/1", "file:///cache/2#video")))
        assertThat(staged).containsExactly(photo.uri, video.uri).inOrder()
        assertThat(deleted).isEmpty()
    }

    @Test
    fun originalAnimatedImage_isImportedAsPhoto() {
        val gif = original("content://media/picker/0/photo/3", MediaType.ANIMATED_IMAGE, "image/gif")

        val outcome = handler.handle(FrameKitResult.Success(gif))

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Edited(listOf("file:///cache/3")))
    }

    @Test
    fun motionMp4_isStagedAsAlbumVideo_andDeleted() {
        val motion = media("motion.mp4", MediaType.VIDEO)

        val outcome = handler.handle(FrameKitResult.Success(motion), isVideo = true)

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Edited(listOf("file:///cache/motion.mp4#video")))
        assertThat(deleted).containsExactly(motion.uri)
    }

    @Test
    fun animatedOrAudioOutputs_areDropped_butDeleted() {
        val gif = media("motion.gif", MediaType.ANIMATED_IMAGE)
        val audio = media("voice.m4a", MediaType.AUDIO)

        val outcome = handler.handle(FrameKitResult.Success(gif, listOf(gif, audio)))

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Failed(R.string.photo_edit_error_generic))
        assertThat(staged).isEmpty()
        assertThat(deleted).containsExactly(gif.uri, audio.uri)
    }

    @Test
    fun share_editedPhoto_isCopiedForSharing_andOutputDeleted() {
        val photo = media("shared.jpg")

        val outcome = handler.handleShare(FrameKitResult.Success(photo))

        assertThat(outcome).isEqualTo(
            PhotoShareOutcome.Ready(Uri.parse("content://com.naury.chageun.photos/photo_share/copy.jpg"), "image/jpeg"),
        )
        assertThat(shared.single()).isEqualTo(photo.uri to "jpg")
        assertThat(deleted).containsExactly(photo.uri)
    }

    @Test
    fun share_originalVideo_isCopied_butNotDeleted() {
        val video = original("file:///data/files/album/album_1.mp4", MediaType.VIDEO, "video/mp4")

        val outcome = handler.handleShare(FrameKitResult.Success(video))

        assertThat((outcome as PhotoShareOutcome.Ready).mimeType).isEqualTo("video/mp4")
        assertThat(shared.single()).isEqualTo(video.uri to "mp4")
        assertThat(deleted).isEmpty()
    }

    @Test
    fun share_copyFailure_reportsError_andStillDeletesOutput() {
        stageFails = true
        val photo = media("shared.jpg")

        val outcome = handler.handleShare(FrameKitResult.Success(photo))

        assertThat(outcome).isEqualTo(PhotoShareOutcome.Failed(R.string.photo_edit_error_generic))
        assertThat(deleted).containsExactly(photo.uri)
        assertThat(handler.handleShare(FrameKitResult.Cancelled)).isEqualTo(PhotoShareOutcome.Cancelled)
    }

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
    fun success_stagesVideoAsVideo_andDeletesIt() {
        val video = media("clip.mp4", MediaType.VIDEO)

        val outcome = handler.handle(FrameKitResult.Success(video), isVideo = true)

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Edited(listOf("file:///cache/clip.mp4#video")))
        assertThat(deleted).containsExactly(video.uri)
    }

    @Test
    fun videoFailure_talksAboutTheVideo() {
        val outcome = handler.handle(
            FrameKitResult.Failure(EditorError(EditorErrorCode.UNSUPPORTED_FORMAT)),
            isVideo = true,
        )

        assertThat(outcome).isEqualTo(PhotoEditOutcome.Failed(R.string.video_edit_error_unsupported))
        assertThat(EditorErrorCode.SOURCE_UNAVAILABLE.messageRes(isVideo = true))
            .isEqualTo(R.string.video_edit_error_unavailable)
        assertThat(EditorErrorCode.SOURCE_TOO_LARGE.messageRes(isVideo = true))
            .isEqualTo(R.string.video_edit_error_too_large)
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
        val crashing = PhotoEditResultHandler(stage = { _, _ -> error("disk") }, deleteOutput = { deleted += it })
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

    private fun original(uri: String, type: MediaType, mime: String) = EditedMedia(
        uri = Uri.parse(uri),
        mediaType = type,
        width = 100,
        height = 100,
        durationMs = null,
        mimeType = mime,
        fileSize = 10,
        isOriginal = true,
    )

    private fun media(name: String, type: MediaType = MediaType.IMAGE) = EditedMedia(
        uri = Uri.parse("content://com.naury.chageun.framekit.files/exports/$name"),
        mediaType = type,
        width = 100,
        height = 100,
        durationMs = null,
        mimeType = when (type) {
            MediaType.IMAGE -> "image/jpeg"
            MediaType.VIDEO -> "video/mp4"
            else -> "application/pdf"
        },
        fileSize = 10,
    )
}
