package com.naury.chageun.core.ui.photo

import com.google.common.truth.Truth.assertThat
import com.naury.framekit.android.input.EditorInput
import com.naury.framekit.android.input.MediaKind
import com.naury.framekit.core.document.ScanMode
import com.naury.framekit.core.pdf.PdfPageSize
import com.naury.framekit.core.validation.ValidationResult
import com.naury.framekit.image.export.ImageFormat
import com.naury.framekit.image.export.MetadataPolicy
import com.naury.framekit.ui.config.EditorUiConfig
import com.naury.framekit.ui.image.contract.ImageTool
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReceiptRequestsTest {

    private val ui = EditorUiConfig()

    @Test
    fun scan_opensDocumentScanner_upToAttachableCount_inOriginalColor() {
        val request = receiptScanRequest(10, ui)
        val capped = receiptScanRequest(500, ui)

        assertThat(request.maxPages).isEqualTo(10)
        assertThat(capped.maxPages).isEqualTo(MAX_EDIT_ITEMS)
        assertThat(request.autoCapture).isTrue()
        assertThat(request.scanMode).isEqualTo(ScanMode.COLOR)
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun scanTools_keepDocumentFirst_andNoDecorationTools() {
        val request = receiptEditRequest(ReceiptSource.Pick(10), 10, ReceiptTools.SCAN, ui)

        assertThat(request.config.enabledTools.toList()).containsExactly(
            ImageTool.DOCUMENT,
            ImageTool.CROP,
            ImageTool.ROTATE,
            ImageTool.ADJUST,
            ImageTool.FILTER,
            ImageTool.PRIVACY,
        ).inOrder()
        assertThat(request.config.enabledTools).containsNoneOf(
            ImageTool.STICKER,
            ImageTool.DRAW,
            ImageTool.TEXT,
            ImageTool.CUTOUT,
            ImageTool.WATERMARK,
        )
        assertThat(request.config.maxImageCount).isEqualTo(10)
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun receiptExport_isJpeg_upToHighQualityEdge_withoutLocation() {
        val export = receiptEditRequest(ReceiptSource.Pick(1), 1, ReceiptTools.ATTACH, ui).export

        assertThat(export.format).isEqualTo(ImageFormat.JPEG)
        assertThat(export.maxWidth).isEqualTo(PhotoFiles.MAX_EDIT_EDGE_PX)
        assertThat(export.maxHeight).isEqualTo(PhotoFiles.MAX_EDIT_EDGE_PX)
        assertThat(export.quality).isAtLeast(90)
        assertThat(export.metadataPolicy).isEqualTo(MetadataPolicy.SAFE)
    }

    @Test
    fun attach_keepsDocumentFirst_andAllowsMarkingDamage() {
        val picked = receiptEditRequest(ReceiptSource.Pick(50), 50, ReceiptTools.ATTACH, ui)
        val existing =
            receiptEditRequest(ReceiptSource.Existing("/data/files/attachments/a.jpg"), 1, ReceiptTools.ATTACH, ui)

        assertThat(picked.input).isEqualTo(EditorInput.Pick(MediaKind.IMAGE, MAX_EDIT_ITEMS))
        assertThat(picked.config.enabledTools.first()).isEqualTo(ImageTool.DOCUMENT)
        assertThat(picked.config.enabledTools).containsAtLeast(ImageTool.DRAW, ImageTool.TEXT, ImageTool.PRIVACY)
        assertThat(picked.config.enabledTools).containsNoneOf(ImageTool.STICKER, ImageTool.CUTOUT)
        assertThat(existing.input).isEqualTo(EditorInput.FileSource("/data/files/attachments/a.jpg"))
        assertThat(existing.config.maxImageCount).isEqualTo(1)
        assertThat(picked.validate()).isEqualTo(ValidationResult.Valid)
        assertThat(existing.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun pdf_keepsAttachmentOrder_asOneA4DocumentWithText() {
        val paths = listOf("/data/files/attachments/c.jpg", "/data/files/attachments/a.jpg", "/data/files/b.jpg")

        val request = receiptPdfRequest(paths, ui)

        assertThat(request.input).isEqualTo(EditorInput.Multiple(paths.map { EditorInput.FileSource(it) }))
        assertThat(request.export.format).isEqualTo(ImageFormat.PDF)
        assertThat(request.export.pdf.pageSize).isEqualTo(PdfPageSize.A4)
        assertThat(request.export.pdf.combinePages).isTrue()
        assertThat(request.export.pdf.recognizeText).isTrue()
        assertThat(request.config.maxImageCount).isEqualTo(3)
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun pdf_offersOnlyFixingAndHidingTools() {
        val tools = receiptPdfRequest(listOf("/data/files/a.jpg"), ui).config.enabledTools

        assertThat(tools.toList())
            .containsExactly(ImageTool.DOCUMENT, ImageTool.CROP, ImageTool.ROTATE, ImageTool.PRIVACY)
            .inOrder()
    }
}
