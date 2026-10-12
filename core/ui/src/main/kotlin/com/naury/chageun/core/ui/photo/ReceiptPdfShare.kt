package com.naury.chageun.core.ui.photo

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import com.naury.chageun.core.ui.R
import com.naury.framekit.android.output.AppFileOutputStore
import com.naury.framekit.android.result.EditedMedia
import com.naury.framekit.android.result.FrameKitResult
import com.naury.framekit.core.model.MediaType
import java.io.File
import java.util.UUID

/** 영수증 PDF 편집 결과를 공유 창이 바로 쓸 수 있게 정리한 것. */
internal sealed interface ReceiptPdfOutcome {
    /** [uri]는 앱 FileProvider로 노출한, 이름을 붙인 PDF다. */
    data class Ready(val uri: Uri) : ReceiptPdfOutcome

    data object Cancelled : ReceiptPdfOutcome

    data class Failed(@param:StringRes val message: Int) : ReceiptPdfOutcome
}

/**
 * 편집기가 만든 PDF를 공유용 파일로 옮기고 원래 결과는 바로 지운다. 인식한 글자(`recognizedText`)는 PDF 안에만
 * 두고 읽지도 저장하지도 않는다. 글자 인식을 건너뛴 경고가 붙어도 PDF는 그대로 보낸다.
 *
 * @param stage PDF 결과 하나를 공유용 파일로 옮기고 그 URI를 준다. 옮기지 못하면 null.
 * @param deleteOutput 편집기 결과 파일을 지운다.
 */
internal class ReceiptPdfResultHandler(private val stage: (Uri) -> Uri?, private val deleteOutput: (Uri) -> Unit) {
    fun handle(result: FrameKitResult): ReceiptPdfOutcome = when (result) {
        FrameKitResult.Cancelled -> ReceiptPdfOutcome.Cancelled
        is FrameKitResult.Failure -> ReceiptPdfOutcome.Failed(result.error.code.messageRes())
        is FrameKitResult.Success -> {
            val shared = stageFirstPdf(result.outputs)
            shared?.let { ReceiptPdfOutcome.Ready(it) } ?: ReceiptPdfOutcome.Failed(R.string.receipt_pdf_error)
        }
    }

    /** 첫 PDF만 옮기고, 옮기다 실패해도 받은 결과는 모두 지운다. */
    private fun stageFirstPdf(outputs: List<EditedMedia>): Uri? {
        var shared: Uri? = null
        outputs.forEach { media ->
            try {
                if (shared == null && media.mediaType == MediaType.DOCUMENT) shared = stage(media.uri)
            } finally {
                deleteOutput(media.uri)
            }
        }
        return shared
    }
}

/**
 * 공유 창으로 보낼 영수증 PDF. 받는 앱이 언제 다 읽을지 알 수 없어 보낸 뒤 바로 지우지 않고,
 * 다음에 기록 화면을 열거나 PDF를 만들 때 하루 지난 것을 정리한다.
 */
object ReceiptShareFiles {
    private const val DIRECTORY = "receipt_share"
    private const val STALE_MILLIS = 24 * 60 * 60 * 1000L
    private const val PDF_MIME_TYPE = "application/pdf"
    private val UNSAFE_NAME = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    /**
     * [source]를 `receipt_share/<임의 폴더>/[fileName]`으로 복사하고 FileProvider URI를 준다. 폴더를 나눠
     * 같은 날 두 번 보내도 앞의 파일을 덮어쓰지 않는다. 읽지 못하면 null이고 반쯤 쓴 파일은 남기지 않는다.
     */
    fun copyIn(context: Context, source: Uri, fileName: String): Uri? {
        val folder = File(File(context.cacheDir, DIRECTORY), UUID.randomUUID().toString()).apply { mkdirs() }
        val target = File(folder, safeFileName(fileName))
        val copied = runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        }.getOrNull()
        if (copied == null || target.length() == 0L) {
            folder.deleteRecursively()
            return null
        }
        return FileProvider.getUriForFile(context, PhotoFiles.authority(context), target)
    }

    /** 하루 지난 공유 PDF와, 앱이 결과를 옮기기 전에 끝나 남은 편집기 결과 파일을 지운다. */
    fun cleanStale(context: Context, now: Long = System.currentTimeMillis()) {
        cleanStale(File(context.cacheDir, DIRECTORY), AppFileOutputStore(context).directory, now)
    }

    /**
     * [shareDirectory] 아래 하루 지난 폴더와 [exportDirectory]의 하루 지난 결과 파일을 지운다.
     * 편집기가 쓰는 중인 숨김 `.partial` 파일은 편집기가 직접 정리하므로 건드리지 않는다.
     */
    internal fun cleanStale(shareDirectory: File, exportDirectory: File, now: Long) {
        shareDirectory.listFiles()?.filter {
            now - it.lastModified() > STALE_MILLIS
        }?.forEach { it.deleteRecursively() }
        exportDirectory.listFiles()
            ?.filter { it.isFile && !it.name.startsWith(".") && now - it.lastModified() > STALE_MILLIS }
            ?.forEach(File::delete)
    }

    /** 파일 이름에 쓸 수 없는 문자를 밑줄로 바꾼다. */
    internal fun safeFileName(name: String): String = name.replace(UNSAFE_NAME, "_").ifBlank { "receipts.pdf" }

    /**
     * 받는 앱이 PDF를 읽을 수 있게 권한을 함께 넘긴다. 공유 창도 파일 이름·미리보기를 읽으므로 바깥 Intent에도
     * ClipData를 넣는다.
     */
    fun chooser(uri: Uri, title: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = PDF_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title).apply {
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
