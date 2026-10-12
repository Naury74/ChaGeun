package com.naury.chageun.core.ui.photo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.ui.R
import com.naury.framekit.android.result.EditedMedia
import com.naury.framekit.android.result.EditorError
import com.naury.framekit.android.result.EditorErrorCode
import com.naury.framekit.android.result.ExportWarning
import com.naury.framekit.android.result.FrameKitResult
import com.naury.framekit.core.model.MediaType
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class ReceiptPdfShareTest {
    // FileProvider는 경로 설정을 프로세스 전체에 캐시하는데, Robolectric은 테스트마다 데이터 폴더가 달라진다.
    // 앞선 테스트가 만든 캐시가 남아 있으면 이 테스트의 cache 폴더를 찾지 못하므로 비우고 시작한다.
    @Before
    fun clearFileProviderCache() {
        FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
            .let { (it.get(null) as MutableMap<*, *>).clear() }
    }

    @get:Rule
    val temp = TemporaryFolder()

    private val staged = mutableListOf<Uri>()
    private val deleted = mutableListOf<Uri>()
    private val handler = ReceiptPdfResultHandler(
        stage = { uri ->
            staged += uri
            Uri.parse("content://app.photos/receipt_share/x/receipts.pdf")
        },
        deleteOutput = { deleted += it },
    )

    @Test
    fun success_sharesTheStagedPdf_andDeletesEditorOutput() {
        val pdf = pdf("out.pdf")

        val outcome = handler.handle(FrameKitResult.Success(pdf))

        assertThat(outcome).isEqualTo(
            ReceiptPdfOutcome.Ready(Uri.parse("content://app.photos/receipt_share/x/receipts.pdf")),
        )
        assertThat(staged).containsExactly(pdf.uri)
        assertThat(deleted).containsExactly(pdf.uri)
    }

    @Test
    fun skippedTextRecognition_stillSharesThePdf() {
        val pdf = pdf("out.pdf", warnings = listOf(ExportWarning.TEXT_RECOGNITION_SKIPPED))

        assertThat(handler.handle(FrameKitResult.Success(pdf))).isInstanceOf(ReceiptPdfOutcome.Ready::class.java)
    }

    @Test
    fun recognizedText_isNeitherKeptNorLogged() {
        ShadowLog.clear()
        val secret = "카드번호 1234-5678-9012-3456 12가3456"
        val pdf = pdf("out.pdf", text = secret)

        val outcome = handler.handle(FrameKitResult.Success(pdf))

        assertThat(outcome.toString()).doesNotContain("1234")
        assertThat(ShadowLog.getLogs().map { it.msg.orEmpty() }.filter { it.contains("1234") }).isEmpty()
    }

    @Test
    fun noPdfOrStagingFailure_reportsError_andLeavesNoOutput() {
        val failing = ReceiptPdfResultHandler(stage = { null }, deleteOutput = { deleted += it })
        val pdf = pdf("out.pdf")
        val photo = pdf("photo.jpg", type = MediaType.IMAGE)

        assertThat(failing.handle(FrameKitResult.Success(pdf)))
            .isEqualTo(ReceiptPdfOutcome.Failed(R.string.receipt_pdf_error))
        assertThat(handler.handle(FrameKitResult.Success(photo)))
            .isEqualTo(ReceiptPdfOutcome.Failed(R.string.receipt_pdf_error))
        assertThat(deleted).containsExactly(pdf.uri, photo.uri)
    }

    @Test
    fun cancelAndFailure_touchNothing() {
        assertThat(handler.handle(FrameKitResult.Cancelled)).isEqualTo(ReceiptPdfOutcome.Cancelled)
        assertThat(handler.handle(FrameKitResult.Failure(EditorError(EditorErrorCode.SOURCE_UNAVAILABLE))))
            .isEqualTo(ReceiptPdfOutcome.Failed(R.string.photo_edit_error_unavailable))
        assertThat(staged).isEmpty()
        assertThat(deleted).isEmpty()
    }

    @Test
    fun chooser_sendsPdf_withReadPermission() {
        val uri = Uri.parse("content://app.photos/receipt_share/x/receipts.pdf")

        val chooser = ReceiptShareFiles.chooser(uri, "Share")
        val send = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)!!

        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        assertThat(chooser.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
        assertThat(chooser.clipData!!.getItemAt(0).uri).isEqualTo(uri)
        assertThat(send.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(send.type).isEqualTo("application/pdf")
        assertThat(IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java)).isEqualTo(uri)
        assertThat(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
    }

    @Test
    fun copyIn_namesTheFileAfterTheRecord_andExposesItThroughAppProvider() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(temp.root, "framekit.pdf").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        val uri = ReceiptShareFiles.copyIn(context, Uri.fromFile(source), "차근_영수증_2026-10-12.pdf")!!

        assertThat(uri.authority).isEqualTo(PhotoFiles.authority(context))
        assertThat(uri.lastPathSegment).isEqualTo("차근_영수증_2026-10-12.pdf")
        val copied = File(context.cacheDir, "receipt_share").walkTopDown().single { it.isFile }
        assertThat(copied.readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test
    fun copyIn_unreadableSource_leavesNothing() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val uri = ReceiptShareFiles.copyIn(context, Uri.fromFile(File(temp.root, "missing.pdf")), "a.pdf")

        assertThat(uri).isNull()
        assertThat(File(context.cacheDir, "receipt_share").walkTopDown().filter { it.isFile }.toList()).isEmpty()
    }

    @Test
    fun cleanStale_removesDayOldSharesAndOrphanedOutputs_butKeepsRecentAndPartial() {
        val now = 10 * DAY
        val share = temp.newFolder("receipt_share")
        val exports = temp.newFolder("exports")
        val oldShare = File(share, "old").apply { mkdirs() }.also { File(it, "a.pdf").writeText("x") }
        val newShare = File(share, "new").apply { mkdirs() }.also { File(it, "b.pdf").writeText("x") }
        val oldOutput = File(exports, "o.pdf").apply { writeText("x") }
        val partial = File(exports, ".p.partial").apply { writeText("x") }
        val newOutput = File(exports, "n.jpg").apply { writeText("x") }
        listOf(oldShare, oldOutput, partial).forEach { it.setLastModified(now - 2 * DAY) }
        listOf(newShare, newOutput).forEach { it.setLastModified(now - DAY / 2) }

        ReceiptShareFiles.cleanStale(share, exports, now)

        assertThat(oldShare.exists()).isFalse()
        assertThat(oldOutput.exists()).isFalse()
        assertThat(newShare.exists()).isTrue()
        assertThat(newOutput.exists()).isTrue()
        assertThat(partial.exists()).isTrue()
    }

    @Test
    fun safeFileName_replacesPathCharacters() {
        assertThat(ReceiptShareFiles.safeFileName("a/b:c.pdf")).isEqualTo("a_b_c.pdf")
        assertThat(ReceiptShareFiles.safeFileName("차근_영수증_2026-10-12.pdf")).isEqualTo("차근_영수증_2026-10-12.pdf")
    }

    private fun pdf(
        name: String,
        type: MediaType = MediaType.DOCUMENT,
        warnings: List<ExportWarning> = emptyList(),
        text: String? = null,
    ) = EditedMedia(
        uri = Uri.parse("content://app.framekit.files/exports/$name"),
        mediaType = type,
        width = 595,
        height = 842,
        durationMs = null,
        mimeType = if (type == MediaType.DOCUMENT) "application/pdf" else "image/jpeg",
        fileSize = 100,
        warnings = warnings,
        pageCount = 1,
        recognizedText = text,
    )

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
