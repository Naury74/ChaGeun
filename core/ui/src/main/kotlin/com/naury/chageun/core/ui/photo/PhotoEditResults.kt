package com.naury.chageun.core.ui.photo

import android.net.Uri
import androidx.annotation.StringRes
import com.naury.chageun.core.ui.R
import com.naury.framekit.android.result.EditedMedia
import com.naury.framekit.android.result.EditorErrorCode
import com.naury.framekit.android.result.FrameKitResult
import com.naury.framekit.core.model.MediaType

/** 편집기 결과를 화면이 바로 쓸 수 있게 정리한 것. */
internal sealed interface PhotoEditOutcome {
    /** [uris]는 앱 캐시에 옮겨 둔 JPEG나 MP4다. 저장소의 가져오기에 그대로 넘긴다. */
    data class Edited(val uris: List<String>) : PhotoEditOutcome

    data object Cancelled : PhotoEditOutcome

    data class Failed(@param:StringRes val message: Int) : PhotoEditOutcome
}

/** 공유하기 전 편집기 결과. */
internal sealed interface PhotoShareOutcome {
    /** [uri]는 다른 앱이 읽을 수 있는 앱 캐시 사본이다. */
    data class Ready(val uri: Uri, val mimeType: String) : PhotoShareOutcome

    data object Cancelled : PhotoShareOutcome

    data class Failed(@param:StringRes val message: Int) : PhotoShareOutcome
}

/**
 * 편집기가 만든 파일을 캐시로 옮기고 원래 파일은 바로 지운다. 결과 파일은 SDK가 지우지 않으므로
 * 성공·실패와 상관없이 받은 결과는 모두 지운다. 경로와 URI는 개인 정보라 어디에도 기록하지 않는다.
 *
 * "편집 없이 넣기"를 고른 원본(`isOriginal`)은 Photo Picker URI 그대로라 지우지 않는다. 읽기 권한이 앱 프로세스와 함께
 * 사라질 수 있어 결과를 받자마자 캐시로 복사한다.
 *
 * @param stage 결과 하나를 캐시로 옮긴다. 두 번째 값이 true면 동영상이다. 옮기지 못하면 null.
 * @param deleteOutput 편집기 결과 파일을 지운다.
 * @param stageShare 공유할 결과 하나를 다른 앱이 읽을 수 있는 캐시 사본으로 만든다. 두 번째 값은 확장자다.
 */
internal class PhotoEditResultHandler(
    private val stage: (Uri, Boolean) -> String?,
    private val deleteOutput: (Uri) -> Unit,
    private val stageShare: (Uri, String) -> Uri? = { _, _ -> null },
) {
    /** [isVideo]는 동영상 편집기에서 온 결과인지다. 오류 안내 문구를 사진·동영상에 맞게 고른다. */
    fun handle(result: FrameKitResult, isVideo: Boolean = false): PhotoEditOutcome = when (result) {
        FrameKitResult.Cancelled -> PhotoEditOutcome.Cancelled
        is FrameKitResult.Failure -> PhotoEditOutcome.Failed(result.error.code.messageRes(isVideo))
        is FrameKitResult.Success -> {
            val staged = result.outputs.mapNotNull(::stageOne)
            if (staged.isEmpty()) {
                PhotoEditOutcome.Failed(
                    R.string.photo_edit_error_generic,
                )
            } else {
                PhotoEditOutcome.Edited(staged)
            }
        }
    }

    /** 공유할 결과 하나(편집했으면 그 파일, 아니면 원본)를 캐시 사본으로 만든다. 편집기 결과 파일은 바로 지운다. */
    fun handleShare(result: FrameKitResult): PhotoShareOutcome = when (result) {
        FrameKitResult.Cancelled -> PhotoShareOutcome.Cancelled
        is FrameKitResult.Failure -> PhotoShareOutcome.Failed(result.error.code.messageRes())
        is FrameKitResult.Success -> {
            val media = result.output
            val isVideo = media.isVideo()
            val copy = try {
                stageShare(media.uri, if (isVideo) VIDEO_EXTENSION else PHOTO_EXTENSION)
            } finally {
                result.outputs.filterNot { it.isOriginal }.forEach { deleteOutput(it.uri) }
            }
            copy?.let { PhotoShareOutcome.Ready(it, if (isVideo) VIDEO_MIME else PHOTO_MIME) }
                ?: PhotoShareOutcome.Failed(R.string.photo_edit_error_generic)
        }
    }

    private fun stageOne(media: EditedMedia): String? = try {
        when {
            // 원본은 사진이든 움직이는 이미지든 저장소가 다시 인코딩해 가져간다.
            media.isOriginal -> stage(media.uri, media.isVideo())
            media.mediaType == MediaType.IMAGE -> stage(media.uri, false)
            media.mediaType == MediaType.VIDEO -> stage(media.uri, true)
            else -> null
        }
    } finally {
        if (!media.isOriginal) deleteOutput(media.uri)
    }
}

private fun EditedMedia.isVideo() = mediaType == MediaType.VIDEO || mimeType.startsWith("video/")

private const val PHOTO_EXTENSION = "jpg"
private const val VIDEO_EXTENSION = "mp4"
private const val PHOTO_MIME = "image/jpeg"
private const val VIDEO_MIME = "video/mp4"

/** 사용자가 할 수 있는 일이 같은 오류끼리 묶어 짧은 안내 하나로 보여 준다. 사진·동영상에 따라 낱말만 다르다. */
@StringRes
internal fun EditorErrorCode.messageRes(isVideo: Boolean = false): Int = when (this) {
    EditorErrorCode.PERMISSION_DENIED,
    EditorErrorCode.SOURCE_UNAVAILABLE,
    -> if (isVideo) R.string.video_edit_error_unavailable else R.string.photo_edit_error_unavailable

    EditorErrorCode.INVALID_SOURCE,
    EditorErrorCode.UNSUPPORTED_FORMAT,
    EditorErrorCode.DECODE_FAILED,
    -> if (isVideo) R.string.video_edit_error_unsupported else R.string.photo_edit_error_unsupported

    EditorErrorCode.SOURCE_TOO_LARGE,
    EditorErrorCode.INSUFFICIENT_MEMORY,
    -> if (isVideo) R.string.video_edit_error_too_large else R.string.photo_edit_error_too_large

    EditorErrorCode.INSUFFICIENT_STORAGE -> R.string.photo_edit_error_storage

    else -> R.string.photo_edit_error_generic
}
