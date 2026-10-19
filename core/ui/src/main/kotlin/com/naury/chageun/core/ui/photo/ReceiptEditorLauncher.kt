package com.naury.chageun.core.ui.photo

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.ui.R
import com.naury.chageun.core.ui.launchExternal
import com.naury.framekit.android.output.FrameKitOutputs
import com.naury.framekit.android.result.FrameKitResult
import com.naury.framekit.scanner.DocumentScanContract
import com.naury.framekit.ui.image.contract.ImageEditorContract
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 영수증을 찍거나 고쳐 첨부하고, 첨부한 사진을 PDF로 묶어 보내는 손잡이. [rememberReceiptEditorLauncher]로 만든다.
 *
 * 영수증을 찍을 때는 문서 스캐너가 테두리를 찾아 자동으로 찍고 반듯하게 편 쪽을 바로 첨부한다.
 * 고른 사진을 고칠 때는 편집기가 문서 보정을 맨 앞에 두고 열린다.
 */
@Stable
class ReceiptEditorLauncher internal constructor(
    private val openScanner: (Int, String?) -> Unit,
    private val openEditor: (ReceiptSource, Int, String?) -> Unit,
    private val openPdf: (List<String>, LocalDate) -> Unit,
) {
    /** 카메라로 영수증을 [maxItems]쪽까지 이어 찍어 펴서 첨부한다. 앱 안 카메라를 거부하면 카메라 앱으로 한 장을 찍는다. */
    fun scan(maxItems: Int, tag: String? = null) {
        if (maxItems > 0) openScanner(maxItems, tag)
    }

    /** 시스템 Photo Picker로 [maxItems]장까지 고르게 한 뒤 편집한다. */
    fun pickAndEdit(maxItems: Int, tag: String? = null) {
        if (maxItems > 0) openEditor(ReceiptSource.Pick(maxItems), maxItems, tag)
    }

    /** 첨부한 사진 한 장을 고친다. 결과도 한 장이다. */
    fun edit(file: File, tag: String? = null) = openEditor(ReceiptSource.Existing(file.absolutePath), 1, tag)

    /** [files]를 이 순서대로 한 PDF로 묶고, 가릴 곳을 고친 뒤 공유 창으로 보낸다. 파일 이름에는 [date]를 넣는다. */
    fun sharePdf(files: List<File>, date: LocalDate) {
        if (files.isNotEmpty()) openPdf(files.map { it.absolutePath }, date)
    }
}

/**
 * 앱 테마·언어에 맞춘 영수증 편집기를 준비한다. 사진 편집이 끝나면 [onEdited]가 메인 스레드에서 불리고,
 * PDF는 편집이 끝나면 바로 공유 창을 띄운다. 접거나 펴도 결과를 받을 수 있게 화면 배치와 상관없는 자리에서 부른다.
 */
@Composable
fun rememberReceiptEditorLauncher(onEdited: (EditedPhotos) -> Unit): ReceiptEditorLauncher {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val latestOnEdited by rememberUpdatedState(onEdited)
    var pendingTag by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPdfName by rememberSaveable { mutableStateOf<String?>(null) }
    val shareTitle = stringResource(R.string.receipt_pdf_share_title)
    val fileNameFormat = stringResource(R.string.receipt_pdf_file_name)
    LaunchedEffect(appContext) { withContext(Dispatchers.IO) { ReceiptShareFiles.cleanStale(appContext) } }

    val onResult: (FrameKitResult) -> Unit = { result ->
        val tag = pendingTag
        pendingTag = null
        scope.launch {
            when (val outcome = withContext(Dispatchers.IO) { photoResultHandler(appContext).handle(result) }) {
                is PhotoEditOutcome.Edited -> latestOnEdited(EditedPhotos(outcome.uris, tag))
                is PhotoEditOutcome.Failed -> context.toast(outcome.message)
                PhotoEditOutcome.Cancelled -> Unit
            }
        }
    }
    val editLauncher = rememberLauncherForActivityResult(ImageEditorContract(), onResult)
    val scanLauncher = rememberLauncherForActivityResult(DocumentScanContract(), onResult)
    val pdfLauncher = rememberLauncherForActivityResult(ImageEditorContract()) { result ->
        val name = pendingPdfName ?: fileNameFormat.format(LocalDate.now().toString())
        pendingPdfName = null
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { pdfResultHandler(appContext, name).handle(result) }
            context.showPdfOutcome(outcome, shareTitle)
        }
    }
    val ui = photoEditUi(MaterialTheme.colorScheme, stringResource(R.string.photo_edit_locale_tag))
    val latestUi by rememberUpdatedState(ui)
    return remember(editLauncher, scanLauncher, pdfLauncher) {
        ReceiptEditorLauncher(
            openScanner = { maxItems, tag ->
                pendingTag = tag
                context.launchReceiptEditor { scanLauncher.launch(receiptScanRequest(maxItems, latestUi)) }
            },
            openEditor = { source, maxItems, tag ->
                pendingTag = tag
                context.launchReceiptEditor {
                    editLauncher.launch(receiptEditRequest(source, maxItems, ReceiptTools.ATTACH, latestUi))
                }
            },
            openPdf = { paths, date ->
                pendingPdfName = fileNameFormat.format(date.toString())
                context.launchReceiptEditor { pdfLauncher.launch(receiptPdfRequest(paths, latestUi)) }
            },
        )
    }
}

private fun Context.launchReceiptEditor(launch: () -> Unit) {
    PhotoFiles.cleanStale(this)
    launchExternal(launch)
}

private fun Context.showPdfOutcome(outcome: ReceiptPdfOutcome, shareTitle: String) {
    when (outcome) {
        is ReceiptPdfOutcome.Ready -> launchExternal {
            startActivity(ReceiptShareFiles.chooser(outcome.uri, shareTitle))
        }
        is ReceiptPdfOutcome.Failed -> toast(outcome.message)
        ReceiptPdfOutcome.Cancelled -> Unit
    }
}

private fun Context.toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

private fun photoResultHandler(context: Context) = PhotoEditResultHandler(
    stage = { uri, _ -> PhotoFiles.copyIn(context, uri) },
    deleteOutput = { uri -> FrameKitOutputs.deleteOutput(context, uri) },
)

private fun pdfResultHandler(context: Context, fileName: String) = ReceiptPdfResultHandler(
    stage = { uri -> ReceiptShareFiles.copyIn(context, uri, fileName) },
    deleteOutput = { uri -> FrameKitOutputs.deleteOutput(context, uri) },
)
