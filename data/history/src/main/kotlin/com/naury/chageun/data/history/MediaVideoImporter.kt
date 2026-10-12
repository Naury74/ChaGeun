package com.naury.chageun.data.history

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 동영상을 다시 인코딩하지 않고 영상·소리 트랙만 새 MP4로 옮긴다. MediaMuxer에 위치를 따로 넘기지 않으므로
 * 원본의 GPS 위치(`©xyz`), 기기 정보, 위치를 담는 시간 메타데이터 트랙은 새 파일에 남지 않는다.
 * 사진 편집기(Media3 Transformer)로 고친 동영상도 원본 위치를 그대로 옮겨 적으므로 같은 길을 거친다.
 *
 * 화면 방향은 회전 값으로 다시 적고, 포스터는 앞부분(1초나 길이의 10% 중 이른 쪽)에서 뽑는다.
 */
internal class MediaVideoImporter @Inject constructor(@param:ApplicationContext private val context: Context) :
    VideoImporter {

    override fun isVideo(sourceUri: String): Boolean {
        val uri = sourceUri.toUri()
        val type = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            // 형식을 알려 주지 않거나 오류를 내는 제공자는 사진으로 보고 사진 가져오기에 맡긴다.
            runCatching { context.contentResolver.getType(uri) }.getOrNull()
        } else {
            val extension = File(uri.path ?: sourceUri).extension.lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: VIDEO_EXTENSIONS[extension]
        }
        return type?.startsWith(VIDEO_MIME_PREFIX) == true
    }

    override fun import(
        sourceUri: String,
        target: File,
        poster: File,
        maxDurationMs: Long,
        maxBytes: Long,
    ): VideoImport {
        val uri = sourceUri.toUri()
        val probe = runCatching { probe(uri) }.getOrNull() ?: return VideoImport.Failed
        if (probe.durationMs > maxDurationMs || sourceSize(uri) > maxBytes) return VideoImport.TooLarge
        target.parentFile?.mkdirs()
        val result = runCatching {
            remux(uri, target, probe.rotation)
            when {
                target.length() > maxBytes -> VideoImport.TooLarge
                !writePoster(uri, probe.posterTimeUs, poster) -> VideoImport.Failed
                else -> VideoImport.Imported(probe.toImported(target, poster))
            }
        }.getOrDefault(VideoImport.Failed)
        if (result !is VideoImport.Imported) {
            target.delete()
            poster.delete()
        }
        return result
    }

    private fun probe(uri: Uri): VideoProbe? = MediaMetadataRetriever().useRetriever(uri) {
        if (extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != HAS_VIDEO) return@useRetriever null
        val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val rotation = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val sideways = rotation % HALF_TURN != 0
        VideoProbe(
            durationMs = duration,
            rotation = rotation,
            width = if (sideways) height else width,
            height = if (sideways) width else height,
            takenOn = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.let(::parseVideoDate),
        )
    }

    /** 원본 크기. 알 수 없으면 0이고, 이때는 옮긴 파일 크기로 다시 확인한다. */
    private fun sourceSize(uri: Uri): Long = runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
    }.getOrNull()?.takeIf { it != AssetFileDescriptor.UNKNOWN_LENGTH } ?: 0L

    private fun remux(uri: Uri, target: File, rotation: Int) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val muxer = MediaMuxer(target.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                val tracks = (0 until extractor.trackCount).mapNotNull { index ->
                    val format = extractor.getTrackFormat(index)
                    // 영상·소리가 아닌 트랙(위치·센서 기록 같은 시간 메타데이터)은 옮기지 않는다.
                    if (!format.isAudioOrVideo()) return@mapNotNull null
                    extractor.selectTrack(index)
                    index to muxer.addTrack(format)
                }.toMap()
                check(tracks.isNotEmpty())
                muxer.setOrientationHint(rotation)
                muxer.start()
                copySamples(extractor, muxer, tracks)
                muxer.stop()
            } finally {
                muxer.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun copySamples(extractor: MediaExtractor, muxer: MediaMuxer, tracks: Map<Int, Int>) {
        val capacity = tracks.keys.maxOf { extractor.getTrackFormat(it).maxInputSize() }
        val buffer = ByteBuffer.allocate(capacity)
        val info = MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            val track = tracks[extractor.sampleTrackIndex]
            // 오디오 앞 여백처럼 0보다 이른 샘플은 MP4에 쓸 수 없어 건너뛴다.
            if (track != null && extractor.sampleTime >= 0) {
                val flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                info.set(0, size, extractor.sampleTime, flags)
                muxer.writeSampleData(track, buffer, info)
            }
            extractor.advance()
        }
    }

    /** 앞부분 한 장면을 긴 변 [POSTER_EDGE_PX]로 줄여 쓴다. 그 자리에서 못 뽑으면 첫 장면을 쓴다. */
    private fun writePoster(uri: Uri, timeUs: Long, poster: File): Boolean =
        MediaMetadataRetriever().useRetriever(uri) {
            val frame = getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return@useRetriever false
            val (width, height) = ImageSizing.fit(frame.width, frame.height, POSTER_EDGE_PX)
            val scaled = if (width == frame.width && height == frame.height) frame else frame.scale(width, height)
            poster.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, POSTER_QUALITY, it) }
            true
        } ?: false

    private fun <T> MediaMetadataRetriever.useRetriever(uri: Uri, block: MediaMetadataRetriever.() -> T): T? = try {
        setDataSource(context, uri)
        block()
    } finally {
        release()
    }

    private fun MediaFormat.isAudioOrVideo(): Boolean {
        val mime = getString(MediaFormat.KEY_MIME).orEmpty()
        return mime.startsWith(VIDEO_MIME_PREFIX) || mime.startsWith(AUDIO_MIME_PREFIX)
    }

    private fun MediaFormat.maxInputSize(): Int = if (containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
        maxOf(getInteger(MediaFormat.KEY_MAX_INPUT_SIZE), MIN_SAMPLE_BUFFER)
    } else {
        DEFAULT_SAMPLE_BUFFER
    }

    private data class VideoProbe(
        val durationMs: Long,
        val rotation: Int,
        val width: Int,
        val height: Int,
        val takenOn: LocalDate?,
    ) {
        // 검은 첫 장면을 피하려고 조금 들어간 곳에서 뽑는다. 아주 짧은 영상은 길이의 10%다.
        val posterTimeUs: Long get() = minOf(POSTER_MAX_OFFSET_MS, durationMs / POSTER_FRACTION) * MICROS_PER_MILLI

        fun toImported(target: File, poster: File) =
            ImportedVideo(target, poster, target.length(), durationMs, width, height, takenOn)
    }

    private companion object {
        const val VIDEO_MIME_PREFIX = "video/"
        const val AUDIO_MIME_PREFIX = "audio/"
        const val HAS_VIDEO = "yes"
        const val HALF_TURN = 180
        const val POSTER_EDGE_PX = 720
        const val POSTER_QUALITY = 85
        const val POSTER_MAX_OFFSET_MS = 1_000L
        const val POSTER_FRACTION = 10
        const val MICROS_PER_MILLI = 1_000L
        const val MIN_SAMPLE_BUFFER = 1 shl 20
        const val DEFAULT_SAMPLE_BUFFER = 8 shl 20

        // 확장자 표가 비어 있는 기기도 있어 앱이 만드는 형식은 직접 적어 둔다.
        val VIDEO_EXTENSIONS = mapOf("mp4" to "video/mp4", "m4v" to "video/mp4", "3gp" to "video/3gpp")
    }
}

/**
 * MediaMetadataRetriever의 날짜("yyyyMMdd'T'HHmmss.SSS'Z'")에서 날짜만 읽는다. 날짜를 비워 둔 MP4는 1904년이나
 * 1970년으로 나오므로 그보다 이른 해는 모르는 것으로 본다.
 */
internal fun parseVideoDate(value: String): LocalDate? = value.take(VIDEO_DATE_LENGTH)
    .let { runCatching { LocalDate.parse(it, VIDEO_DATE) }.getOrNull() }
    ?.takeIf { it.year > EARLIEST_VIDEO_YEAR }

private const val VIDEO_DATE_LENGTH = 8
private const val EARLIEST_VIDEO_YEAR = 1970
private val VIDEO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
