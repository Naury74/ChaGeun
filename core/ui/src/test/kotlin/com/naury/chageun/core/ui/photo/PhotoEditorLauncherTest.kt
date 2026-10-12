package com.naury.chageun.core.ui.photo

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
import com.naury.framekit.ui.config.ThemeMode
import com.naury.framekit.ui.image.contract.ImageEditorRequest
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

/** 편집 화면은 Robolectric에서 띄울 수 없으므로 결과 registry를 바꿔 요청과 결과 처리만 확인한다. */
@RunWith(RobolectricTestRunner::class)
class PhotoEditorLauncherTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val requests = mutableListOf<ImageEditorRequest>()
    private var lastRequestCode = -1
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            requests += input as ImageEditorRequest
            lastRequestCode = requestCode
        }
    }
    private val owner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = registry
    }

    private lateinit var launcher: PhotoEditorLauncher
    private val edited = mutableListOf<EditedPhotos>()

    private fun show(darkTheme: Boolean = false) = composeRule.setContent {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            ChageunTheme(darkTheme = darkTheme) {
                launcher = rememberPhotoEditorLauncher { edited += it }
            }
        }
    }

    @Test
    fun edit_opensAppFileByPath_inAppTheme() {
        show(darkTheme = true)
        val file = File(context.filesDir, "attachments/a.jpg")

        composeRule.runOnIdle { launcher.edit(file) }

        val request = requests.single()
        assertThat(request.input).isEqualTo(EditorInput.FileSource(file.absolutePath))
        assertThat(request.config.maxImageCount).isEqualTo(1)
        assertThat(request.ui.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(request.ui.localeTag).isEqualTo("en")
    }

    @Test
    fun pickAndEdit_letsUserPickUpToLimit() {
        show()

        composeRule.runOnIdle { launcher.pickAndEdit(maxItems = 3) }

        assertThat(requests.single().input).isEqualTo(EditorInput.Pick(MediaKind.IMAGE, 3))
    }

    @Test
    fun nothingToEdit_doesNotOpenEditor() {
        show()

        composeRule.runOnIdle {
            launcher.edit(emptyList())
            launcher.pickAndEdit(maxItems = 0)
        }

        assertThat(requests).isEmpty()
    }

    @Test
    fun success_handsCachedCopyAndTag_andRemovesEditorOutput() {
        show()
        val output = File(context.cacheDir, "framekit-output.jpg").apply { writeText("edited") }
        composeRule.runOnIdle { launcher.edit(File(context.filesDir, "a.jpg"), tag = "replace:a1") }

        composeRule.runOnIdle { registry.dispatchResult(lastRequestCode, FrameKitResult.Success(media(output))) }
        composeRule.waitUntil(TIMEOUT_MS) { edited.isNotEmpty() }

        val result = edited.single()
        assertThat(result.tag).isEqualTo("replace:a1")
        val staged = File(requireNotNull(result.uris.single().toUri().path))
        assertThat(staged.parentFile?.name).isEqualTo("photo_input")
        assertThat(staged.readText()).isEqualTo("edited")
    }

    @Test
    fun failure_showsShortNotice_andCancelDoesNothing() {
        show()
        composeRule.runOnIdle { launcher.pickAndEdit(maxItems = 1) }

        composeRule.runOnIdle {
            registry.dispatchResult(lastRequestCode, FrameKitResult.Failure(EditorError(EditorErrorCode.DECODE_FAILED)))
        }
        composeRule.waitUntil(TIMEOUT_MS) {
            ShadowLooper.idleMainLooper()
            ShadowToast.getTextOfLatestToast() != null
        }
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("This photo can't be edited. Try another one.")

        composeRule.runOnIdle { launcher.pickAndEdit(maxItems = 1) }
        composeRule.runOnIdle { registry.dispatchResult(lastRequestCode, FrameKitResult.Cancelled) }
        composeRule.waitForIdle()
        assertThat(edited).isEmpty()
        assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
    }

    private fun media(file: File) = EditedMedia(
        uri = file.toUri(),
        mediaType = MediaType.IMAGE,
        width = 10,
        height = 10,
        durationMs = null,
        mimeType = "image/jpeg",
        fileSize = file.length(),
    )

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
