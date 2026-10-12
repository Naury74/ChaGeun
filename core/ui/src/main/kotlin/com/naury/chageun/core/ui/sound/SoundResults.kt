package com.naury.chageun.core.ui.sound

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import com.naury.chageun.core.ui.R
import com.naury.framekit.android.result.EditedMedia
import com.naury.framekit.android.result.EditorErrorCode
import com.naury.framekit.android.result.FrameKitResult
import com.naury.framekit.core.model.MediaType
import java.io.File

/** 녹음 편집기 결과를 화면이 바로 쓸 수 있게 정리한 것. */
internal sealed interface SoundEditOutcome {
    /** [uri]는 앱 캐시에 옮겨 둔 m4a다. 저장소의 가져오기에 그대로 넘긴다. */
    data class Recorded(val uri: String) : SoundEditOutcome

    data object Cancelled : SoundEditOutcome

    data class Failed(@param:StringRes val message: Int) : SoundEditOutcome
}

/**
 * 편집기가 만든 m4a를 캐시로 옮기고 원래 파일은 바로 지운다. 결과·녹음 파일은 SDK가 지우지 않으므로
 * 성공·취소·실패와 상관없이 이번 편집의 녹음 파일도 함께 정리한다. 경로와 URI는 어디에도 기록하지 않는다.
 *
 * @param stage 결과 하나를 캐시로 옮긴다. 옮기지 못하면 null.
 * @param deleteOutput 편집기 결과 파일을 지운다.
 * @param cleanRecordings 이번 편집에서 녹음한 파일(files/framekit/recordings)을 지운다.
 */
internal class SoundEditResultHandler(
    private val stage: (Uri) -> String?,
    private val deleteOutput: (Uri) -> Unit,
    private val cleanRecordings: () -> Unit,
) {
    fun handle(result: FrameKitResult): SoundEditOutcome = try {
        when (result) {
            FrameKitResult.Cancelled -> SoundEditOutcome.Cancelled
            is FrameKitResult.Failure -> SoundEditOutcome.Failed(result.error.code.soundMessageRes())
            is FrameKitResult.Success -> stageFirstSound(result.outputs)
        }
    } finally {
        cleanRecordings()
    }

    private fun stageFirstSound(outputs: List<EditedMedia>): SoundEditOutcome {
        val staged = try {
            outputs.firstOrNull { it.isSound() }?.let { stage(it.uri) }
        } finally {
            outputs.filterNot { it.isOriginal }.forEach { deleteOutput(it.uri) }
        }
        return staged?.let(SoundEditOutcome::Recorded) ?: SoundEditOutcome.Failed(R.string.photo_edit_error_generic)
    }
}

private fun EditedMedia.isSound() = mediaType == MediaType.AUDIO || mimeType.startsWith("audio/")

/** 사용자가 할 수 있는 일이 같은 오류끼리 묶어 짧은 안내 하나로 보여 준다. */
@StringRes
internal fun EditorErrorCode.soundMessageRes(): Int = when (this) {
    EditorErrorCode.PERMISSION_DENIED,
    EditorErrorCode.SOURCE_UNAVAILABLE,
    -> R.string.sound_edit_error_unavailable

    EditorErrorCode.INVALID_SOURCE,
    EditorErrorCode.UNSUPPORTED_FORMAT,
    EditorErrorCode.DECODE_FAILED,
    -> R.string.sound_edit_error_unsupported

    EditorErrorCode.INSUFFICIENT_STORAGE -> R.string.photo_edit_error_storage

    else -> R.string.photo_edit_error_generic
}

/**
 * FrameKit 녹음 화면이 남기는 녹음 파일. 편집을 이어 가는 데 쓰여 편집기가 지우지 않으므로 앱이 지운다.
 * 파일 이름으로는 어느 편집의 녹음인지 알 수 없어, 편집을 시작한 시각 뒤에 쓴 파일만 이번 편집의 것으로 본다.
 * 다른 편집기(앨범 동영상의 소리 도구 등)가 멈춰 둔 세션은 FrameKit이 7일까지 이어 주므로 그보다 오래된 것만 지운다.
 */
internal object FrameKitRecordings {
    private const val DIRECTORY = "framekit/recordings"
    private const val STALE_MILLIS = 7 * 24 * 60 * 60 * 1000L

    // 파일 시각은 초 단위로 내려가는 파일 시스템도 있어 조금 앞에서부터 본다.
    private const val CLOCK_MARGIN_MILLIS = 2_000L

    fun directory(context: Context) = File(context.filesDir, DIRECTORY)

    /** [since]는 이번 편집을 연 시각이다. 모르면 오래된 파일만 지운다. */
    fun clean(context: Context, since: Long?, now: Long = System.currentTimeMillis()) {
        directory(context).listFiles()
            ?.filter { file ->
                val modified = file.lastModified()
                (since != null && modified >= since - CLOCK_MARGIN_MILLIS) || now - modified > STALE_MILLIS
            }
            ?.forEach(File::deleteRecursively)
    }
}
