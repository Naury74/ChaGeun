package com.naury.chageun.core.ui.photo

import android.net.Uri
import androidx.annotation.StringRes
import com.naury.chageun.core.ui.R
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

/**
 * 편집기가 만든 파일을 캐시로 옮기고 원래 파일은 바로 지운다. 결과 파일은 SDK가 지우지 않으므로
 * 성공·실패와 상관없이 받은 결과는 모두 지운다. 경로와 URI는 개인 정보라 어디에도 기록하지 않는다.
 *
 * @param stage 결과 하나를 캐시로 옮긴다. 두 번째 값이 true면 동영상이다. 옮기지 못하면 null.
 * @param deleteOutput 편집기 결과 파일을 지운다.
 */
internal class PhotoEditResultHandler(
    private val stage: (Uri, Boolean) -> String?,
    private val deleteOutput: (Uri) -> Unit,
) {
    /** [isVideo]는 동영상 편집기에서 온 결과인지다. 오류 안내 문구를 사진·동영상에 맞게 고른다. */
    fun handle(result: FrameKitResult, isVideo: Boolean = false): PhotoEditOutcome = when (result) {
        FrameKitResult.Cancelled -> PhotoEditOutcome.Cancelled
        is FrameKitResult.Failure -> PhotoEditOutcome.Failed(result.error.code.messageRes(isVideo))
        is FrameKitResult.Success -> {
            val staged = result.outputs.mapNotNull { media ->
                try {
                    when (media.mediaType) {
                        MediaType.IMAGE -> stage(media.uri, false)
                        MediaType.VIDEO -> stage(media.uri, true)
                        else -> null
                    }
                } finally {
                    deleteOutput(media.uri)
                }
            }
            if (staged.isEmpty()) {
                PhotoEditOutcome.Failed(
                    R.string.photo_edit_error_generic,
                )
            } else {
                PhotoEditOutcome.Edited(staged)
            }
        }
    }
}

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
