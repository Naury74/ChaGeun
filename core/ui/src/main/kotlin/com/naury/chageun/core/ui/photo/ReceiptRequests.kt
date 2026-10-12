package com.naury.chageun.core.ui.photo

import com.naury.framekit.android.input.EditorInput
import com.naury.framekit.android.input.MediaKind
import com.naury.framekit.core.pdf.PdfPageSize
import com.naury.framekit.image.export.ImageExportConfig
import com.naury.framekit.image.export.ImageFormat
import com.naury.framekit.image.export.MetadataPolicy
import com.naury.framekit.image.export.PdfOptions
import com.naury.framekit.ui.config.EditorUiConfig
import com.naury.framekit.ui.image.contract.ImageEditorConfig
import com.naury.framekit.ui.image.contract.ImageEditorRequest
import com.naury.framekit.ui.image.contract.ImageTool
import java.io.File

/** 영수증 편집기에 넘길 사진. */
internal sealed interface ReceiptSource {
    /** 시스템 카메라 앱으로 찍은 뒤 바로 편집한다. */
    data object Capture : ReceiptSource

    /** 시스템 Photo Picker로 [maxItems]장까지 고른 뒤 편집한다. */
    data class Pick(val maxItems: Int) : ReceiptSource

    /** 이미 첨부한 사진 한 장(앱 저장소의 절대 경로). */
    data class Existing(val path: String) : ReceiptSource
}

/**
 * 영수증·첨부 사진에 맞춘 도구 묶음. 편집기는 넘긴 순서대로 도구 줄을 그리므로 문서 보정을 맨 앞에 둔다.
 * 문서 보정은 사진이 문서로 판별될 때만 보이므로 일반 사진을 열어도 빈 도구가 생기지 않는다.
 */
internal object ReceiptTools {
    /** 영수증 스캔. 꾸미기 도구(텍스트·스티커·그리기·배경 제거·워터마크)는 숨긴다. */
    val SCAN: Set<ImageTool> = linkedSetOf(
        ImageTool.DOCUMENT,
        ImageTool.CROP,
        ImageTool.ROTATE,
        ImageTool.ADJUST,
        ImageTool.FILTER,
        ImageTool.PRIVACY,
    )

    /** 기록에 붙이는 사진. 흠집 자리를 표시할 수 있게 그리기·텍스트는 남긴다. */
    val ATTACH: Set<ImageTool> = LinkedHashSet(SCAN + listOf(ImageTool.DRAW, ImageTool.TEXT))

    /** 영수증 PDF. 카드 번호·차량번호를 가릴 수 있게 가리기는 남기고 나머지는 최소로 둔다. */
    val PDF: Set<ImageTool> = linkedSetOf(ImageTool.DOCUMENT, ImageTool.CROP, ImageTool.ROTATE, ImageTool.PRIVACY)
}

/**
 * 영수증용 사진 편집기 요청. 작은 글씨가 남도록 긴 변을 고화질 상한(4096px)까지 두고, 위치 정보는 남기지 않는다.
 * [maxItems]는 편집기 안에서 쪽을 더할 수 있는 상한이다.
 */
internal fun receiptEditRequest(
    source: ReceiptSource,
    maxItems: Int,
    tools: Set<ImageTool>,
    ui: EditorUiConfig,
): ImageEditorRequest {
    val limit = maxItems.coerceIn(1, MAX_EDIT_ITEMS)
    val input = when (source) {
        ReceiptSource.Capture -> EditorInput.Capture(MediaKind.IMAGE)
        is ReceiptSource.Pick -> EditorInput.Pick(MediaKind.IMAGE, source.maxItems.coerceIn(1, limit))
        is ReceiptSource.Existing -> EditorInput.FileSource(File(source.path).absolutePath)
    }
    return ImageEditorRequest(
        input = input,
        config = ImageEditorConfig(enabledTools = tools, maxImageCount = limit),
        export = ImageExportConfig(
            format = ImageFormat.JPEG,
            quality = RECEIPT_JPEG_QUALITY,
            maxWidth = PhotoFiles.MAX_EDIT_EDGE_PX,
            maxHeight = PhotoFiles.MAX_EDIT_EDGE_PX,
            metadataPolicy = MetadataPolicy.SAFE,
        ),
        ui = ui,
    )
}

/**
 * 첨부 사진들을 첨부한 순서대로 한 PDF(A4)로 묶는 요청. 글자 인식 모듈이 있으면 검색·복사되는 글자 층을 넣는다.
 * 쪽 이미지는 메일로 보내기 좋게 조금 낮은 품질로 넣는다.
 */
internal fun receiptPdfRequest(paths: List<String>, ui: EditorUiConfig): ImageEditorRequest {
    val limit = paths.size.coerceIn(1, MAX_EDIT_ITEMS)
    return ImageEditorRequest(
        input = EditorInput.Multiple(paths.take(limit).map { EditorInput.FileSource(File(it).absolutePath) }),
        config = ImageEditorConfig(enabledTools = ReceiptTools.PDF, maxImageCount = limit),
        export = ImageExportConfig(
            format = ImageFormat.PDF,
            quality = PDF_JPEG_QUALITY,
            pdf = PdfOptions(pageSize = PdfPageSize.A4, combinePages = true, recognizeText = true),
        ),
        ui = ui,
    )
}

private const val RECEIPT_JPEG_QUALITY = 95
private const val PDF_JPEG_QUALITY = 85
