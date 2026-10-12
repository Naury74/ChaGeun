package com.naury.chageun.core.ui.photo

import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.naury.framekit.android.input.EditorInput
import com.naury.framekit.android.input.MediaKind
import com.naury.framekit.image.export.ImageExportConfig
import com.naury.framekit.image.export.ImageFormat
import com.naury.framekit.image.export.MetadataPolicy
import com.naury.framekit.ui.config.EditorPalette
import com.naury.framekit.ui.config.EditorUiConfig
import com.naury.framekit.ui.config.ThemeMode
import com.naury.framekit.ui.image.contract.ImageEditorConfig
import com.naury.framekit.ui.image.contract.ImageEditorRequest
import java.io.File

/** 편집기에 넘길 사진. FrameKit 타입을 모듈 밖에 드러내지 않으려고 따로 둔다. */
internal sealed interface PhotoEditSource {
    /** 앱 저장소의 파일이나 캐시 파일, 또는 Photo Picker가 준 URI. */
    data class Existing(val uris: List<String>) : PhotoEditSource

    /** 시스템 Photo Picker로 [maxItems]장까지 고른 뒤 바로 편집한다. */
    data class Pick(val maxItems: Int) : PhotoEditSource
}

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
    return ImageEditorRequest(
        input = input,
        config = ImageEditorConfig(maxImageCount = limit),
        // SAFE는 촬영일을 남기고 GPS·기기 정보는 빼므로 앨범 날짜 정리가 그대로 된다.
        export = ImageExportConfig(
            format = ImageFormat.JPEG,
            quality = JPEG_QUALITY,
            metadataPolicy = MetadataPolicy.SAFE,
        ),
        ui = ui,
    )
}

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

/** 기록 하나에 붙일 수 있는 사진 수보다 넉넉하고, 편집기 기본값(20)과 같다. */
internal const val MAX_EDIT_ITEMS = 20
private const val JPEG_QUALITY = 95

// 앱의 카드·시트에 쓰는 medium 모양(16dp)과 맞춘다.
private const val CORNER_RADIUS_DP = 16
private const val HALF_LUMINANCE = 0.5f
