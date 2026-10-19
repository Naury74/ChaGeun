package com.naury.chageun.core.ui.sound

import android.content.Context
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
import com.naury.chageun.core.designsystem.component.ChageunToasts
import com.naury.chageun.core.ui.R
import com.naury.chageun.core.ui.launchExternal
import com.naury.chageun.core.ui.photo.PhotoFiles
import com.naury.chageun.core.ui.photo.photoEditUi
import com.naury.framekit.android.output.FrameKitOutputs
import com.naury.framekit.ui.audioeditor.contract.AudioEditorContract
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 녹음·편집을 마친 소리.
 *
 * @property uri 앱 캐시에 옮겨 둔 m4a URI. 첨부의 소리 가져오기에 그대로 넘긴다. 캐시 파일은 하루 뒤 정리된다.
 * @property tag 녹음을 시작할 때 넘긴 값. 어느 기록·소리였는지를 화면이 다시 만들어져도 알 수 있게 담는다.
 */
data class RecordedSound(val uri: String, val tag: String? = null)

/**
 * FrameKit 녹음 편집기를 여는 손잡이. [rememberSoundRecorderLauncher]로 만든다.
 * 새로 녹음할 때 [record], 다른 앱에서 녹음해 둔 파일을 넣을 때 [pickFile], 붙인 소리를 고칠 때 [edit]를 쓴다.
 */
@Stable
class SoundRecorderLauncher internal constructor(private val open: (SoundEditSource, String?) -> Unit) {
    fun record(tag: String? = null) = open(SoundEditSource.Record, tag)

    fun pickFile(tag: String? = null) = open(SoundEditSource.Pick, tag)

    fun edit(file: File, tag: String? = null) = open(SoundEditSource.Existing(file.absolutePath), tag)
}

/**
 * 앱 테마·언어에 맞춘 녹음 편집기를 준비한다. 편집이 끝나면 [onRecorded]가 메인 스레드에서 불린다.
 * 마이크 권한은 녹음 버튼을 처음 누를 때 편집기가 묻는다. 접거나 펴도 결과를 받을 수 있게 화면 배치와 상관없는 자리에서 부른다.
 *
 * @param maxRecordingMs 한 번 녹음할 수 있는 최대 길이. 닿으면 녹음이 저절로 멈춘다.
 */
@Composable
fun rememberSoundRecorderLauncher(maxRecordingMs: Long, onRecorded: (RecordedSound) -> Unit): SoundRecorderLauncher {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val latestOnRecorded by rememberUpdatedState(onRecorded)
    var pendingTag by rememberSaveable { mutableStateOf<String?>(null) }
    // 프로세스가 다시 만들어져도 이번 편집의 녹음 파일을 가릴 수 있게 연 시각을 남긴다.
    var startedAt by rememberSaveable { mutableStateOf<Long?>(null) }
    val ui = photoEditUi(MaterialTheme.colorScheme, stringResource(R.string.photo_edit_locale_tag))
    val latestUi by rememberUpdatedState(ui)
    val latestMax by rememberUpdatedState(maxRecordingMs)
    val launcher = rememberLauncherForActivityResult(AudioEditorContract()) { result ->
        val tag = pendingTag
        val since = startedAt
        pendingTag = null
        startedAt = null
        scope.launch {
            val handler = soundResultHandler(appContext, since)
            when (val outcome = withContext(Dispatchers.IO) { handler.handle(result) }) {
                is SoundEditOutcome.Recorded -> latestOnRecorded(RecordedSound(outcome.uri, tag))
                is SoundEditOutcome.Failed -> context.showNotice(outcome.message)
                SoundEditOutcome.Cancelled -> Unit
            }
        }
    }
    return remember(launcher) {
        SoundRecorderLauncher { source, tag ->
            pendingTag = tag
            startedAt = System.currentTimeMillis()
            PhotoFiles.cleanStale(appContext)
            FrameKitRecordings.clean(appContext, since = null)
            context.launchExternal { launcher.launch(soundEditRequest(source, latestMax, latestUi)) }
        }
    }
}

private fun Context.showNotice(message: Int) = ChageunToasts.error(getString(message))

private fun soundResultHandler(context: Context, since: Long?) = SoundEditResultHandler(
    stage = { uri -> PhotoFiles.copyIn(context, uri, SOUND_EXTENSION) },
    deleteOutput = { uri -> FrameKitOutputs.deleteOutput(context, uri) },
    cleanRecordings = { FrameKitRecordings.clean(context, since) },
)

private const val SOUND_EXTENSION = "m4a"
