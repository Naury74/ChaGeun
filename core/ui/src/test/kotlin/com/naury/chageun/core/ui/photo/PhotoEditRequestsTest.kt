package com.naury.chageun.core.ui.photo

import android.net.Uri
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.naury.framekit.android.input.EditorInput
import com.naury.framekit.android.input.MediaKind
import com.naury.framekit.core.validation.ValidationResult
import com.naury.framekit.image.export.MetadataPolicy
import com.naury.framekit.ui.config.EditorUiConfig
import com.naury.framekit.ui.config.ThemeMode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoEditRequestsTest {

    private val ui = EditorUiConfig()

    @Test
    fun appFile_opensByPath_withoutUriPermission() {
        val byPath = photoEditRequest(PhotoEditSource.Existing(listOf("/data/files/attachments/a.jpg")), 1, ui)
        val byFileUri = photoEditRequest(PhotoEditSource.Existing(listOf("file:///data/cache/b.jpg")), 1, ui)

        assertThat(byPath.input).isEqualTo(EditorInput.FileSource("/data/files/attachments/a.jpg"))
        assertThat(byFileUri.input).isEqualTo(EditorInput.FileSource("/data/cache/b.jpg"))
        assertThat(byPath.config.maxImageCount).isEqualTo(1)
    }

    @Test
    fun pickedUris_openTogether() {
        val uris = listOf("content://media/picker/1", "content://media/picker/2")

        val request = photoEditRequest(PhotoEditSource.Existing(uris), 10, ui)

        assertThat(request.input).isEqualTo(EditorInput.Multiple(uris.map { EditorInput.UriSource(Uri.parse(it)) }))
        assertThat(request.config.maxImageCount).isEqualTo(10)
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun pick_staysWithinEditorLimit_andIsValid() {
        val request = photoEditRequest(PhotoEditSource.Pick(maxItems = 50), 50, ui)

        assertThat(request.input).isEqualTo(EditorInput.Pick(MediaKind.IMAGE, MAX_EDIT_ITEMS))
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun export_dropsLocation_butKeepsTakenDate() {
        val request = photoEditRequest(PhotoEditSource.Pick(1), 1, ui)

        assertThat(request.export.metadataPolicy).isEqualTo(MetadataPolicy.SAFE)
    }

    @Test
    fun ui_followsAppThemeAndLanguage() {
        val dark = darkColorScheme(primary = Color(0xFF3D7BFF), background = Color(0xFF0D0D0E))
        val light = lightColorScheme(primary = Color(0xFF1F5EFF), background = Color.White)

        val darkUi = photoEditUi(dark, "ko")
        val lightUi = photoEditUi(light, "en")

        assertThat(darkUi.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(darkUi.primaryArgb).isEqualTo(Color(0xFF3D7BFF).toArgb())
        assertThat(darkUi.palette?.backgroundArgb).isEqualTo(Color(0xFF0D0D0E).toArgb())
        assertThat(darkUi.localeTag).isEqualTo("ko")
        assertThat(lightUi.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(lightUi.localeTag).isEqualTo("en")
        assertThat(lightUi.validate()).isEqualTo(ValidationResult.Valid)
    }
}
