package com.naury.chageun.core.ui.photo

import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.naury.framekit.EditorAction
import com.naury.framekit.EditorChooser
import com.naury.framekit.FrameKitRequest
import com.naury.framekit.android.input.EditorInput
import com.naury.framekit.android.input.MediaKind
import com.naury.framekit.core.overlay.Watermark
import com.naury.framekit.image.export.ImageExportConfig
import com.naury.framekit.image.export.ImageFormat
import com.naury.framekit.image.export.MetadataPolicy
import com.naury.framekit.motion.export.MotionExportConfig
import com.naury.framekit.motion.export.MotionFormat
import com.naury.framekit.ui.config.EditorPalette
import com.naury.framekit.ui.config.EditorUiConfig
import com.naury.framekit.ui.config.ThemeMode
import com.naury.framekit.ui.config.WatermarkConfig
import com.naury.framekit.ui.image.contract.ImageEditorConfig
import com.naury.framekit.ui.image.contract.ImageEditorRequest
import com.naury.framekit.ui.motion.contract.MotionEditorConfig
import com.naury.framekit.ui.motion.contract.MotionEditorRequest
import com.naury.framekit.ui.video.contract.VideoEditorConfig
import com.naury.framekit.ui.video.contract.VideoEditorRequest
import com.naury.framekit.video.export.VideoExportConfig
import java.io.File

/** 편집기에 넘길 사진이나 동영상. FrameKit 타입을 모듈 밖에 드러내지 않으려고 따로 둔다. */
internal sealed interface EditSource

/** 사진 편집기에 넘길 사진. */
internal sealed interface PhotoEditSource : EditSource {
    /** 앱 저장소의 파일이나 캐시 파일, 또는 Photo Picker가 준 URI. [watermark]는 워터마크 도구에 미리 채울 문구다. */
    data class Existing(val uris: List<String>, val watermark: WatermarkText? = null) : PhotoEditSource

    /** 시스템 Photo Picker로 [maxItems]장까지 고른 뒤 바로 편집한다. */
    data class Pick(val maxItems: Int) : PhotoEditSource
}

/** 동영상 편집기에 넘길 동영상. 결과는 늘 MP4 하나다. */
internal sealed interface VideoEditSource : EditSource {
    /** 앱 저장소의 파일이나 캐시 파일, 또는 Photo Picker가 준 URI. [watermark]는 워터마크 도구에 미리 채울 문구다. */
    data class Existing(val uri: String, val watermark: WatermarkText? = null) : VideoEditSource

    /** 시스템 Photo Picker로 동영상 하나를 고른 뒤 바로 편집한다. 편집기 안에서 클립을 더 이어 붙일 수 있다. */
    data object Pick : VideoEditSource
}

/**
 * 사진·동영상을 고른 뒤 무엇을 할지 묻는다. 사진은 편집·움직이는 영상 만들기, 동영상은 편집, 둘 다 그대로 넣기를 고를 수 있다.
 * 결과는 편집한 JPEG·MP4이거나, 그대로 넣기를 고른 원본(Photo Picker URI)이다.
 */
internal data class ChooserPickSource(val maxItems: Int) : EditSource

/** 앱 저장소의 사진 여러 장([paths])으로 슬라이드쇼 동영상을 만든다. 결과는 MP4 하나다. */
internal data class SlideshowSource(val paths: List<String>) : EditSource

/** 앱 저장소의 사진이나 동영상 하나를 다른 앱으로 보낸다. 편집해서 보낼지 그대로 보낼지 먼저 묻는다. */
internal data class ShareSource(val path: String, val watermark: WatermarkText? = null) : EditSource

/**
 * [source]를 여는 사진 편집기 요청. [maxItems]는 편집기 안에서 쪽을 더할 수 있는 상한이라, 1이면 쪽 목록이
 * 숨고 결과도 한 장이다. 위치 정보는 저장소가 다시 인코딩하며 지우지만 편집기 결과에도 남기지 않는다.
 */
internal fun photoEditRequest(source: PhotoEditSource, maxItems: Int, ui: EditorUiConfig): ImageEditorRequest {
    val limit = maxItems.coerceIn(1, MAX_EDIT_ITEMS)
    val input = when (source) {
        is PhotoEditSource.Pick -> EditorInput.Pick(MediaKind.IMAGE, source.maxItems.coerceIn(1, limit))
        is PhotoEditSource.Existing -> source.uris.take(limit).map(::editorInput).let { items ->
            items.singleOrNull() ?: EditorInput.Multiple(items)
        }
    }
    val watermark = (source as? PhotoEditSource.Existing)?.watermark
    return ImageEditorRequest(
        input = input,
        config = ImageEditorConfig(maxImageCount = limit, watermark = watermark?.toConfig()),
        export = photoExport(),
        ui = ui,
    )
}

/**
 * [source]를 여는 동영상 편집기 요청. 결과 길이는 [maxDurationMs]를 넘지 않고, 더 긴 원본은 앞부분만 남긴 채로 열린다.
 * 화질은 편집기 기본값(짧은 변 1080px, 30fps)이다. 결과 파일의 위치 정보는 가져올 때 저장소가 지운다.
 */
internal fun videoEditRequest(source: VideoEditSource, maxDurationMs: Long, ui: EditorUiConfig): VideoEditorRequest {
    val input = when (source) {
        is VideoEditSource.Existing -> editorInput(source.uri)
        VideoEditSource.Pick -> EditorInput.Pick(MediaKind.VIDEO, 1)
    }
    val watermark = (source as? VideoEditSource.Existing)?.watermark
    return VideoEditorRequest(
        input = input,
        config = videoConfig(maxDurationMs, MAX_VIDEO_CLIPS, watermark),
        export = VideoExportConfig(),
        ui = ui,
    )
}

/**
 * 고른 뒤 무엇을 할지 묻는 요청. 앨범에 넣는 길이라 PDF·소리는 빼고, 움직이는 영상은 앨범에서 재생되는 MP4로만 저장한다.
 * 편집 결과의 화질·메타데이터 정책은 [photoEditRequest]·[videoEditRequest]와 같다.
 */
internal fun chooserPickRequest(source: ChooserPickSource, maxDurationMs: Long, ui: EditorUiConfig): FrameKitRequest {
    val limit = source.maxItems.coerceIn(1, MAX_EDIT_ITEMS)
    return FrameKitRequest(
        input = EditorInput.Pick(MediaKind.ANY, limit),
        image = ImageEditorConfig(maxImageCount = limit),
        imageExport = photoExport(),
        video = videoConfig(maxDurationMs, MAX_VIDEO_CLIPS, watermark = null),
        motion = MotionEditorConfig(maxImageCount = limit),
        motionExport = MP4_ONLY,
        chooser = EditorChooser.ASK,
        chooserActions = ALBUM_ADD_ACTIONS,
        ui = ui,
    )
}

/** 앱 저장소의 사진들로 슬라이드쇼를 만드는 요청. 앨범에서 재생할 수 있게 MP4로만 저장한다. */
internal fun slideshowRequest(source: SlideshowSource, ui: EditorUiConfig): MotionEditorRequest {
    val paths = source.paths.take(MAX_EDIT_ITEMS)
    return MotionEditorRequest(
        input = EditorInput.Multiple(paths.map(::editorInput)),
        config = MotionEditorConfig(maxImageCount = MAX_EDIT_ITEMS),
        export = MP4_ONLY,
        ui = ui,
    )
}

/**
 * 공유하기 전에 편집할지 그대로 보낼지 묻는 요청. 편집을 고르면 워터마크 도구에 [ShareSource.watermark]가 채워져 있고,
 * 얹을지는 사용자가 정한다. 움직이는 이미지는 앨범에 없으므로 사진 편집기로 연다.
 */
internal fun shareRequest(source: ShareSource, maxDurationMs: Long, ui: EditorUiConfig): FrameKitRequest {
    val watermark = source.watermark?.toConfig()
    return FrameKitRequest(
        input = editorInput(source.path),
        image = ImageEditorConfig(maxImageCount = 1, watermark = watermark),
        imageExport = photoExport(),
        video = videoConfig(maxDurationMs, maxClips = 1, watermark = source.watermark),
        openAnimatedInMotionEditor = false,
        chooser = EditorChooser.ASK,
        chooserActions = SHARE_ACTIONS,
        ui = ui,
    )
}

// SAFE는 촬영일을 남기고 GPS·기기 정보는 빼므로 앨범 날짜 정리가 그대로 된다.
private fun photoExport() = ImageExportConfig(
    format = ImageFormat.JPEG,
    quality = JPEG_QUALITY,
    metadataPolicy = MetadataPolicy.SAFE,
)

private fun videoConfig(maxDurationMs: Long, maxClips: Int, watermark: WatermarkText?): VideoEditorConfig {
    val maxDurationUs = (maxDurationMs * MICROS_PER_MILLI)
        .coerceIn(VideoEditorConfig().minClipDurationUs, VideoEditorConfig.MAX_TIMELINE_US)
    return VideoEditorConfig(
        maxTimelineDurationUs = maxDurationUs,
        maxClipCount = maxClips,
        watermark = watermark?.toConfig(),
    )
}

/**
 * 워터마크 도구에 미리 채울 문구. 얹지는 않고(applyOnOpen = false) 사용자가 도구를 열어 고른다.
 * 편집기 규칙(한 줄 60자, 추천 8개)에 맞게 다듬고, 맞지 않는 문구는 버린다.
 */
internal fun WatermarkText.toConfig(): WatermarkConfig {
    val cleaned = suggestions.map(::oneLine).filter { it.isNotEmpty() }.distinct().take(WatermarkConfig.MAX_SUGGESTIONS)
    return WatermarkConfig(text = oneLine(text), suggestions = cleaned, applyOnOpen = false)
}

private fun oneLine(value: String) = value.replace('\n', ' ').trim().take(Watermark.MAX_TEXT_LENGTH)

/** 앱 안의 파일은 권한 없이 바로 읽게 경로로 넘기고, 그 밖은 URI 그대로 넘긴다. */
private fun editorInput(uri: String): EditorInput {
    val parsed: Uri = uri.toUri()
    val path = parsed.path
    return if (parsed.scheme == "file" && path != null) {
        EditorInput.FileSource(File(path).absolutePath)
    } else if (parsed.scheme == null) {
        EditorInput.FileSource(File(uri).absolutePath)
    } else {
        EditorInput.UriSource(parsed)
    }
}

/**
 * 앱 테마를 편집기 색으로 옮긴다. 다크 여부는 앱 설정(시스템·라이트·다크)을 반영한 배경 밝기로 정하고,
 * 언어는 앱 문구가 실제로 쓰는 언어([localeTag])로 맞춘다.
 */
internal fun photoEditUi(colors: ColorScheme, localeTag: String): EditorUiConfig = EditorUiConfig(
    themeMode = if (colors.background.luminance() < HALF_LUMINANCE) ThemeMode.DARK else ThemeMode.LIGHT,
    primaryArgb = colors.primary.toArgb(),
    cornerRadiusDp = CORNER_RADIUS_DP,
    localeTag = localeTag.ifBlank { null },
    palette = EditorPalette(
        backgroundArgb = colors.background.toArgb(),
        surfaceArgb = colors.surface.toArgb(),
        raisedArgb = colors.surfaceContainerHigh.toArgb(),
        foregroundArgb = colors.onSurface.toArgb(),
        foregroundMutedArgb = colors.onSurfaceVariant.toArgb(),
        onPrimaryArgb = colors.onPrimary.toArgb(),
    ),
)

private val MP4_ONLY = MotionExportConfig(formats = listOf(MotionFormat.MP4), format = MotionFormat.MP4)
private val ALBUM_ADD_ACTIONS = setOf(
    EditorAction.EDIT_PHOTO,
    EditorAction.EDIT_VIDEO,
    EditorAction.MOTION,
    EditorAction.SEND_ORIGINAL,
)
private val SHARE_ACTIONS = setOf(EditorAction.EDIT_PHOTO, EditorAction.EDIT_VIDEO, EditorAction.SEND_ORIGINAL)

/** 기록 하나에 붙일 수 있는 사진 수보다 넉넉하고, 편집기 기본값(20)과 같다. */
internal const val MAX_EDIT_ITEMS = 20
private const val JPEG_QUALITY = 95

/** 편집기에서 이어 붙일 수 있는 클립 수. 앨범 한 칸에 넣는 짧은 영상이라 몇 개면 충분하다. */
private const val MAX_VIDEO_CLIPS = 5
private const val MICROS_PER_MILLI = 1_000L

// 앱의 카드·시트에 쓰는 medium 모양(16dp)과 맞춘다.
private const val CORNER_RADIUS_DP = 16
private const val HALF_LUMINANCE = 0.5f
