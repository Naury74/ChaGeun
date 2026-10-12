package com.naury.chageun.core.ui.photo

import android.net.Uri
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.naury.framekit.EditorAction
import com.naury.framekit.EditorChooser
import com.naury.framekit.android.input.EditorInput
import com.naury.framekit.android.input.MediaKind
import com.naury.framekit.core.validation.ValidationResult
import com.naury.framekit.image.export.MetadataPolicy
import com.naury.framekit.motion.export.MotionFormat
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
    fun video_opensAppFileByPath_andCapsLengthToAlbumLimit() {
        val request = videoEditRequest(VideoEditSource.Existing("/data/files/attachments/album_1.mp4"), 180_000, ui)

        assertThat(request.input).isEqualTo(EditorInput.FileSource("/data/files/attachments/album_1.mp4"))
        assertThat(request.config.maxTimelineDurationUs).isEqualTo(180_000_000)
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun videoPick_picksOneVideo_andIsValid() {
        val request = videoEditRequest(VideoEditSource.Pick, 180_000, ui)

        assertThat(request.input).isEqualTo(EditorInput.Pick(MediaKind.VIDEO, 1))
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
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

    @Test
    fun chooserPick_asksWhatToDo_withAlbumActionsOnly() {
        val request = chooserPickRequest(ChooserPickSource(maxItems = 50), 180_000, ui)

        assertThat(request.input).isEqualTo(EditorInput.Pick(MediaKind.ANY, MAX_EDIT_ITEMS))
        assertThat(request.chooser).isEqualTo(EditorChooser.ASK)
        assertThat(request.chooserActions).containsExactly(
            EditorAction.EDIT_PHOTO,
            EditorAction.EDIT_VIDEO,
            EditorAction.MOTION,
            EditorAction.SEND_ORIGINAL,
        )
        assertThat(request.imageExport.metadataPolicy).isEqualTo(MetadataPolicy.SAFE)
        assertThat(request.video.maxTimelineDurationUs).isEqualTo(180_000_000)
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun chooserPick_savesMotionAsMp4Only_soTheAlbumCanPlayIt() {
        val request = chooserPickRequest(ChooserPickSource(maxItems = 5), 180_000, ui)

        assertThat(request.motionExport.formats).containsExactly(MotionFormat.MP4)
        assertThat(request.motionExport.format).isEqualTo(MotionFormat.MP4)
    }

    @Test
    fun slideshow_opensAppFilesByPath_asMp4Only() {
        val paths = listOf("/data/files/album/a.jpg", "/data/files/album/b.jpg")

        val request = slideshowRequest(SlideshowSource(paths), ui)

        assertThat(request.input).isEqualTo(EditorInput.Multiple(paths.map { EditorInput.FileSource(it) }))
        assertThat(request.export.formats).containsExactly(MotionFormat.MP4)
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun slideshow_keepsWithinEditorLimit() {
        val paths = (1..30).map { "/data/files/album/$it.jpg" }

        val input = slideshowRequest(SlideshowSource(paths), ui).input as EditorInput.Multiple

        assertThat(input.items).hasSize(MAX_EDIT_ITEMS)
    }

    @Test
    fun share_asksEditOrSendAsIs_withWatermarkSuggestionsNotApplied() {
        val watermark = WatermarkText("Kia Sportage · 2026.10.12", listOf("Kia Sportage · 2026.10.12", "차근 정비 기록"))

        val request = shareRequest(ShareSource("/data/files/album/a.jpg", watermark), 180_000, ui)

        assertThat(request.input).isEqualTo(EditorInput.FileSource("/data/files/album/a.jpg"))
        assertThat(request.chooser).isEqualTo(EditorChooser.ASK)
        assertThat(request.chooserActions)
            .containsExactly(EditorAction.EDIT_PHOTO, EditorAction.EDIT_VIDEO, EditorAction.SEND_ORIGINAL)
        val photoWatermark = requireNotNull(request.image.watermark)
        assertThat(photoWatermark.text).isEqualTo("Kia Sportage · 2026.10.12")
        assertThat(photoWatermark.suggestions).containsExactly("Kia Sportage · 2026.10.12", "차근 정비 기록").inOrder()
        assertThat(photoWatermark.applyOnOpen).isFalse()
        assertThat(photoWatermark.locked).isFalse()
        assertThat(request.video.watermark).isEqualTo(photoWatermark)
        assertThat(request.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun albumEdit_prefillsTheSameWatermark() {
        val watermark = WatermarkText("2026.10.12 · 60,000km")

        val photo = photoEditRequest(PhotoEditSource.Existing(listOf("/data/a.jpg"), watermark), 1, ui)
        val video = videoEditRequest(VideoEditSource.Existing("/data/a.mp4", watermark), 180_000, ui)

        assertThat(photo.config.watermark?.text).isEqualTo("2026.10.12 · 60,000km")
        assertThat(video.config.watermark?.text).isEqualTo("2026.10.12 · 60,000km")
        assertThat(photo.validate()).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun watermark_isTrimmedToEditorRules() {
        val long = "x".repeat(80)
        val many = (1..12).map { "Suggestion $it" }

        val config = WatermarkText("two\nlines", listOf(" ", long) + many + many).toConfig()

        assertThat(config.text).isEqualTo("two lines")
        assertThat(config.suggestions).hasSize(8)
        assertThat(config.suggestions.first()).hasLength(60)
        assertThat(config.suggestions).containsNoDuplicates()
        assertThat(config.validate("watermark")).isEqualTo(ValidationResult.Valid)
    }
}
