package com.naury.chageun.core.ui.sound

import android.content.Context
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.framekit.android.input.EditorInput
import com.naury.framekit.android.input.MediaKind
import com.naury.framekit.android.result.EditedMedia
import com.naury.framekit.android.result.EditorError
import com.naury.framekit.android.result.EditorErrorCode
import com.naury.framekit.android.result.FrameKitResult
import com.naury.framekit.core.model.MediaType
import com.naury.framekit.ui.audioeditor.contract.AudioEditorRequest
import com.naury.framekit.ui.audioeditor.contract.AudioTool
import com.naury.framekit.ui.audioeditor.contract.RecordMode
import com.naury.framekit.ui.config.ThemeMode
import java.io.File
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

/** 녹음 화면은 Robolectric에서 띄울 수 없으므로 결과 registry를 바꿔 요청과 결과 처리만 확인한다. */
@RunWith(RobolectricTestRunner::class)
class SoundRecorderLauncherTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val requests = mutableListOf<Any?>()
    private var lastRequestCode = -1
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            requests += input
            lastRequestCode = requestCode
        }
    }
    private val owner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = registry
    }

    private lateinit var launcher: SoundRecorderLauncher
    private val recorded = mutableListOf<RecordedSound>()
    private val recordings get() = FrameKitRecordings.directory(context)

    private fun show(darkTheme: Boolean = false) = composeRule.setContent {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            ChageunTheme(darkTheme = darkTheme) {
                launcher = rememberSoundRecorderLauncher(maxRecordingMs = THREE_MINUTES) { recorded += it }
            }
        }
    }

    @After
    fun tearDown() {
        recordings.deleteRecursively()
    }

    @Test
    fun record_opensRecorderInTapMode_withinThreeMinutes_thenTrimEditor() {
        show(darkTheme = true)

        composeRule.runOnIdle { launcher.record(tag = "sound-add:Repair:r1") }

        val request = requests.single() as AudioEditorRequest
        assertThat(request.input).isEqualTo(EditorInput.Capture(MediaKind.AUDIO))
        assertThat(request.config.recordMode).isEqualTo(RecordMode.TAP)
        assertThat(request.config.maxRecordingMs).isEqualTo(THREE_MINUTES)
        assertThat(request.config.editAfterRecording).isTrue()
        assertThat(request.config.enabledTools).containsExactly(AudioTool.RANGE, AudioTool.VOLUME, AudioTool.SPLIT)
        assertThat(request.export.bitrate).isEqualTo(SOUND_BITRATE)
        assertThat(request.ui.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(request.validate().isValid).isTrue()
    }

    @Test
    fun pickFile_andEdit_openTheAudioEditor() {
        show()
        val file = File(context.filesDir, "attachments/s1.m4a")

        composeRule.runOnIdle {
            launcher.pickFile()
            launcher.edit(file)
        }

        val (picked, existing) = requests.map { it as AudioEditorRequest }
        assertThat(picked.input).isEqualTo(EditorInput.Pick(MediaKind.AUDIO, 1))
        assertThat(existing.input).isEqualTo(EditorInput.FileSource(file.absolutePath))
    }

    @Test
    fun success_handsCachedM4a_andRemovesEditorOutputAndThisSessionsRecordings() {
        show()
        val older = recording("older.m4a", ageMillis = 2 * DAY_MILLIS)
        val stale = recording("stale.m4a", ageMillis = 8 * DAY_MILLIS)
        val output = File(context.cacheDir, "framekit-output.m4a").apply { writeText("trimmed") }
        composeRule.runOnIdle { launcher.record(tag = "sound-add:Repair:r1") }
        // 녹음 화면이 이번 녹음을 남긴다.
        val fresh = recording("fresh.m4a", ageMillis = 0)

        composeRule.runOnIdle { registry.dispatchResult(lastRequestCode, FrameKitResult.Success(audio(output))) }
        composeRule.waitUntil(TIMEOUT_MS) { recorded.isNotEmpty() }

        val result = recorded.single()
        assertThat(result.tag).isEqualTo("sound-add:Repair:r1")
        val staged = File(requireNotNull(result.uri.toUri().path))
        assertThat(staged.extension).isEqualTo("m4a")
        assertThat(staged.readText()).isEqualTo("trimmed")
        assertThat(fresh.exists()).isFalse()
        assertThat(stale.exists()).isFalse()
        // 다른 편집기가 이어 쓸 수 있는 7일 안의 녹음은 남긴다.
        assertThat(older.exists()).isTrue()
    }

    @Test
    fun cancel_stillCleansThisSessionsRecordings_andFailureShowsNotice() {
        show()
        composeRule.runOnIdle { launcher.record() }
        val fresh = recording("fresh.m4a", ageMillis = 0)

        composeRule.runOnIdle { registry.dispatchResult(lastRequestCode, FrameKitResult.Cancelled) }
        composeRule.waitUntil(TIMEOUT_MS) { !fresh.exists() }
        assertThat(recorded).isEmpty()

        composeRule.runOnIdle { launcher.pickFile() }
        composeRule.runOnIdle {
            registry.dispatchResult(lastRequestCode, FrameKitResult.Failure(EditorError(EditorErrorCode.DECODE_FAILED)))
        }
        composeRule.waitUntil(TIMEOUT_MS) {
            ShadowLooper.idleMainLooper()
            ShadowToast.getTextOfLatestToast() != null
        }
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("This sound file can't be edited. Try another one.")
    }

    @Test
    fun handler_deletesOutput_evenWhenStagingFails() {
        val deleted = mutableListOf<String>()
        var cleaned = 0
        val handler = SoundEditResultHandler(
            stage = { null },
            deleteOutput = { deleted += it.toString() },
            cleanRecordings = { cleaned++ },
        )
        val output = File(context.cacheDir, "out.m4a")

        val outcome = handler.handle(FrameKitResult.Success(audio(output)))

        assertThat(outcome).isInstanceOf(SoundEditOutcome.Failed::class.java)
        assertThat(deleted).containsExactly(output.toUri().toString())
        assertThat(cleaned).isEqualTo(1)
    }

    private fun recording(name: String, ageMillis: Long) = File(recordings, name).apply {
        parentFile?.mkdirs()
        writeText("recording")
        setLastModified(System.currentTimeMillis() - ageMillis)
    }

    private fun audio(file: File) = EditedMedia(
        uri = file.toUri(),
        mediaType = MediaType.AUDIO,
        width = 0,
        height = 0,
        durationMs = 12_000,
        mimeType = "audio/mp4",
        fileSize = file.length(),
    )

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val THREE_MINUTES = 3 * 60 * 1_000L
        const val DAY_MILLIS = 24 * 60 * 60 * 1_000L
    }
}
