package com.naury.chageun.core.ui.photo

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.naury.framekit.ui.image.contract.ImageEditorContract
import com.naury.framekit.ui.image.contract.ImageEditorRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 편집을 마친 사진.
 *
 * @property uris 앱 캐시에 옮겨 둔 JPEG URI. 첨부·대표 사진·앨범의 가져오기에 그대로 넘기면 크기 조정, 위치 정보 제거,
 *   썸네일 생성이 기존과 같게 된다. 캐시 파일은 하루 뒤 정리된다.
 * @property tag 편집을 시작할 때 넘긴 값. 어느 첨부를 고치는 중이었는지처럼, 화면이 다시 만들어져도 알아야 하는 것을 담는다.
 */
data class EditedPhotos(val uris: List<String>, val tag: String? = null)

/**
 * FrameKit 사진 편집기를 여는 손잡이. [rememberPhotoEditorLauncher]로 만든다.
 *
 * 편집기 결과 파일은 캐시로 옮긴 뒤 바로 지우고, 실패하면 짧은 안내를 띄운다. 닫기만 하면 아무것도 하지 않는다.
 * 앨범에서는 사진 한 장을 고칠 때 [edit], 새로 골라 편집해 넣을 때 [pickAndEdit]을 쓰면 된다.
 */
@Stable
class PhotoEditorLauncher internal constructor(private val open: (PhotoEditSource, Int, String?) -> Unit) {
    /** 앱 저장소의 사진 파일 한 장을 편집한다. 결과도 한 장이다. */
    fun edit(file: File, tag: String? = null) = open(PhotoEditSource.Existing(listOf(file.absolutePath)), 1, tag)

    /**
     * 이미 고른 사진들을 한꺼번에 편집한다. `file://`·`content://` URI와 절대 경로를 받는다.
     *
     * @param maxItems 편집기 안에서 사진을 더해도 넘지 않을 결과 수.
     */
    fun edit(uris: List<String>, maxItems: Int = uris.size, tag: String? = null) {
        if (uris.isNotEmpty()) open(PhotoEditSource.Existing(uris), maxOf(maxItems, uris.size), tag)
    }

    /** 시스템 Photo Picker로 [maxItems]장까지 고르게 한 뒤 바로 편집한다. */
    fun pickAndEdit(maxItems: Int, tag: String? = null) {
        if (maxItems > 0) open(PhotoEditSource.Pick(maxItems), maxItems, tag)
    }
}

/**
 * 앱 테마·언어에 맞춘 사진 편집기를 준비한다. 편집이 끝나면 [onEdited]가 메인 스레드에서 불린다.
 * 접거나 펴서, 또는 프로세스가 다시 만들어져도 결과를 받을 수 있게 화면 배치와 상관없는 자리에서 부른다.
 */
@Composable
fun rememberPhotoEditorLauncher(onEdited: (EditedPhotos) -> Unit): PhotoEditorLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnEdited by rememberUpdatedState(onEdited)
    var pendingTag by rememberSaveable { mutableStateOf<String?>(null) }
    val handler = remember(context) { resultHandler(context.applicationContext) }
    val launcher = rememberLauncherForActivityResult(ImageEditorContract()) { result ->
        val tag = pendingTag
        pendingTag = null
        scope.launch {
            when (val outcome = withContext(Dispatchers.IO) { handler.handle(result) }) {
                is PhotoEditOutcome.Edited -> latestOnEdited(EditedPhotos(outcome.uris, tag))
                is PhotoEditOutcome.Failed -> Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
                PhotoEditOutcome.Cancelled -> Unit
            }
        }
    }
    val ui = photoEditUi(MaterialTheme.colorScheme, stringResource(R.string.photo_edit_locale_tag))
    val latestUi by rememberUpdatedState(ui)
    return remember(launcher) {
        PhotoEditorLauncher { source, maxItems, tag ->
            pendingTag = tag
            context.launchEditor(launcher, photoEditRequest(source, maxItems, latestUi))
        }
    }
}

private fun Context.launchEditor(
    launcher: ManagedActivityResultLauncher<ImageEditorRequest, FrameKitResult>,
    request: ImageEditorRequest,
) {
    PhotoFiles.cleanStale(this)
    launchExternal { launcher.launch(request) }
}

private fun resultHandler(context: Context) = PhotoEditResultHandler(
    stage = { uri -> PhotoFiles.copyIn(context, uri) },
    deleteOutput = { uri -> FrameKitOutputs.deleteOutput(context, uri) },
)
