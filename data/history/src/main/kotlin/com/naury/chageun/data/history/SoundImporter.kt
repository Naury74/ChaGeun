package com.naury.chageun.data.history

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** 기록에 붙인 소리 파일. 녹음 편집기가 만든 AAC(m4a)를 그대로 옮긴 것이다. */
internal data class ImportedSound(val file: File, val sizeBytes: Long, val durationMs: Long)

internal sealed interface SoundImport {
    data class Imported(val sound: ImportedSound) : SoundImport

    /** 너무 길거나 크다. 파일은 만들지 않는다. */
    data object TooLong : SoundImport

    /** 읽을 수 없거나 소리 파일이 아니다. 반쯤 쓴 파일은 남기지 않는다. */
    data object Failed : SoundImport
}

/** 실제 소리 파일을 다루지 않고 Repository를 테스트할 수 있도록 추상화했다. */
internal fun interface SoundImporter {
    /** [sourceUri]가 소리만 담은 파일이면 [target]으로 복사한다. [maxDurationMs]보다 길면 [SoundImport.TooLong]. */
    fun import(sourceUri: String, target: File, maxDurationMs: Long): SoundImport
}

/** 첨부가 쓰는 사진·소리 가져오기. 저장소 생성자가 길어지지 않게 묶었다. */
internal class AttachmentImporters @Inject constructor(val image: ImageImporter, val sound: SoundImporter)

/**
 * 녹음 편집기 결과는 이미 다시 인코딩한 m4a라 바이트를 그대로 옮긴다. 옮기기 전에 소리 트랙만 있는지와 길이를 확인한다.
 * 편집기는 끝을 잘라 내기만 하므로 녹음 상한보다 조금 긴 것은 인코더 여백으로 보고 받아 준다.
 */
internal class MediaSoundImporter @Inject constructor(@param:ApplicationContext private val context: Context) :
    SoundImporter {

    override fun import(sourceUri: String, target: File, maxDurationMs: Long): SoundImport {
        val uri = sourceUri.toUri()
        val durationMs = runCatching { probeDuration(uri) }.getOrNull() ?: return SoundImport.Failed
        if (durationMs > maxDurationMs + DURATION_TOLERANCE_MS) return SoundImport.TooLong
        target.parentFile?.mkdirs()
        val result = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            when {
                target.length() == 0L -> SoundImport.Failed
                target.length() > MAX_SOUND_BYTES -> SoundImport.TooLong
                else -> SoundImport.Imported(ImportedSound(target, target.length(), durationMs))
            }
        }.getOrDefault(SoundImport.Failed)
        if (result !is SoundImport.Imported) target.delete()
        return result
    }

    /** 소리 트랙만 있으면 길이(ms), 영상이 있거나 길이를 모르면 null. */
    private fun probeDuration(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == YES
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == YES
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            duration?.takeIf { hasAudio && !hasVideo && it > 0 }
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val YES = "yes"
        const val DURATION_TOLERANCE_MS = 1_000L

        // 3분 AAC는 비트레이트를 높여도 5MB 안쪽이다. 잘못 고른 큰 파일을 막는 여유 있는 상한이다.
        const val MAX_SOUND_BYTES = 20L * 1024 * 1024
    }
}
