package com.naury.chageun.feature.history

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.history.MAX_SOUNDS_PER_RECORD
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.AttachmentKind
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class SoundSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ref = RecordRef(TimelineEventType.Repair, "repair")
    private val detail = RecordDetail.Check(ref, CheckEntry(CheckKind.Repair, LocalDate.of(2026, 10, 12), "Brakes"))
    private val engine = sound("s1", "Engine knock", durationMs = 12_000)
    private val brake = sound("s2", null, durationMs = 65_000)

    private val recorded = mutableListOf<RecordRef>()
    private val picked = mutableListOf<RecordRef>()
    private val reEdited = mutableListOf<Attachment>()
    private val renamed = mutableListOf<Pair<String, String>>()
    private val deleted = mutableListOf<String>()
    private var dismissed = 0
    private val edits = SoundAttachmentEdits(
        onRecord = { recorded += it },
        onPickFile = { picked += it },
        onEdit = { reEdited += it },
        onRename = { id, label -> renamed += id to label },
        onDelete = { deleted += it },
        onDismissNotice = { dismissed++ },
    )
    private val fakeEngine = FakeSoundEngine()

    private fun show(
        sounds: List<Attachment>,
        notice: SoundNotice? = null,
        record: RecordDetail = detail,
        frame: Boolean = false,
    ) = composeRule.setContent {
        val content: @Composable () -> Unit = {
            CompositionLocalProvider(LocalSoundEngineFactory provides { _: Context -> fakeEngine }) {
                HistoryScreen(
                    uiState = HistoryUiState(
                        isLoading = false,
                        selected = record.ref,
                        detail = record,
                        attachments = sounds,
                    ),
                    isTwoPane = false,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = {},
                    onAdd = {},
                    onAddPhotos = {},
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                    soundEdits = edits,
                    soundNotice = notice,
                )
            }
        }
        if (frame) AppFrame { content() } else ChageunTheme { content() }
    }

    @Test
    fun rows_showNameOrOrder_durationAndDate() {
        show(listOf(engine, brake))

        composeRule.onNodeWithText("Engine knock").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sound 2").assertIsDisplayed()
        composeRule.onNodeWithText("0:12 · Oct 12, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("1:05 · Oct 12, 2026").assertIsDisplayed()
    }

    @Test
    fun playButton_readsNameDurationAndAction_andTogglesPlayback() {
        show(listOf(engine, brake))

        composeRule.onNodeWithContentDescription("Engine knock, 12 seconds, Play").performScrollTo().performClick()

        assertThat(fakeEngine.played).containsExactly(engine.filePath)
        composeRule.onNodeWithContentDescription("Engine knock, 12 seconds, Pause").performClick()
        assertThat(fakeEngine.pauses).isEqualTo(1)
        composeRule.onNodeWithContentDescription("Engine knock, 12 seconds, Play").assertIsDisplayed()

        // 다른 소리를 누르면 그 소리를 처음부터 재생한다.
        composeRule.onNodeWithContentDescription("Sound 2, 1 min 5 sec, Play").performClick()
        assertThat(fakeEngine.played).containsExactly(engine.filePath, brake.filePath).inOrder()
    }

    @Test
    fun playback_endsByItself_backToPlay() {
        show(listOf(engine))
        composeRule.onNodeWithContentDescription("Engine knock, 12 seconds, Play").performScrollTo().performClick()

        composeRule.runOnIdle { fakeEngine.finish() }

        composeRule.onNodeWithContentDescription("Engine knock, 12 seconds, Play").assertIsDisplayed()
    }

    @Test
    fun recordAndAddFile_openForThisRecord_untilTheLimit() {
        show(listOf(engine))

        composeRule.onNodeWithText("Record a sound").performScrollTo().performClick()
        composeRule.onNodeWithText("Add a sound file").performScrollTo().performClick()

        assertThat(recorded).containsExactly(ref)
        assertThat(picked).containsExactly(ref)
    }

    @Test
    fun fullRecord_disablesRecording_butStillPlays() {
        show(List(MAX_SOUNDS_PER_RECORD) { sound("s$it", "Clip $it", 3_000) })

        composeRule.onNodeWithText("Record a sound").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Add a sound file").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Clip 0, 3 seconds, Play").assertIsEnabled()
    }

    @Test
    fun more_letsUserNameEditAgainAndDeleteWithConfirmation() {
        show(listOf(engine, brake))

        composeRule.onNodeWithContentDescription("More options for Sound 2").performScrollTo().performClick()
        composeRule.onNodeWithText("Name it").performClick()
        composeRule.onNodeWithText("Name").performTextReplacement("Brake squeal")
        composeRule.onNodeWithText("Save").performClick()
        assertThat(renamed).containsExactly("s2" to "Brake squeal")

        composeRule.onNodeWithContentDescription("More options for Engine knock").performClick()
        composeRule.onNodeWithText("Edit again").performClick()
        assertThat(reEdited).containsExactly(engine)

        composeRule.onNodeWithContentDescription("More options for Engine knock").performClick()
        composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        composeRule.onNodeWithText("Delete this sound?").assertIsDisplayed()
        assertThat(deleted).isEmpty()
        composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        assertThat(deleted).containsExactly("s1")
    }

    @Test
    fun notice_explainsWhy_andCanBeClosed() {
        show(listOf(engine), notice = SoundNotice.TooLong)

        composeRule.onNodeWithText("Sounds longer than 3 minutes can't be added. Trim it and try again.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()

        assertThat(dismissed).isEqualTo(1)
    }

    @Test
    fun fuelRecord_hasNoSoundSection() {
        val fuel = RecordDetail.Fuel(
            RecordRef(TimelineEventType.Fuel, "fuel"),
            FuelEntry(
                LocalDate.of(2026, 8, 3),
                Kilometers(37_120),
                FuelAmounts(70_000, 41_176, 1_700, FuelField.Volume),
                true,
                "S-Oil",
            ),
        )
        show(emptyList(), record = fuel)

        composeRule.onNodeWithText("Record a sound").assertDoesNotExist()
    }

    @Test
    fun soundControls_haveLabelsAndLargeTouchTargets() {
        show(listOf(engine, brake))

        composeRule.onNodeWithText("Record a sound").performScrollTo()
        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsSoundRowsReadable() {
        show(listOf(engine, brake))

        composeRule.onNodeWithText("Engine knock").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Record a sound").performScrollTo().assertIsDisplayed()
        composeRule.assertNoClippedText()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_soundsKo() {
        show(listOf(engine.copy(label = "엔진 소리"), brake), frame = true)
        composeRule.onNodeWithText("소리 파일 추가").performScrollTo()
        composeRule.captureScreen("history_detail_sounds_ko")
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h800dp-night-hdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_soundsKoDark() {
        show(listOf(engine.copy(label = "엔진 소리"), brake), frame = true)
        composeRule.onNodeWithText("소리 파일 추가").performScrollTo()
        composeRule.captureScreen("history_detail_sounds_ko_dark")
    }

    private fun sound(id: String, label: String?, durationMs: Long) = Attachment(
        id = id,
        owner = ref,
        filePath = "/missing/$id.m4a",
        thumbnailPath = "",
        createdAt = Instant.parse("2026-10-12T03:00:00Z"),
        kind = AttachmentKind.Sound,
        durationMs = durationMs,
        label = label,
    )
}

/** 재생 요청만 기록하는 엔진. [finish]로 끝까지 들은 것처럼 만든다. */
private class FakeSoundEngine : SoundEngine {
    val played = mutableListOf<String>()
    var pauses = 0
    private var onEnded: () -> Unit = {}

    override val positionMs: Long = 0L

    override fun play(path: String, onPaused: () -> Unit, onEnded: () -> Unit) {
        played += path
        this.onEnded = onEnded
    }

    override fun pause() {
        pauses++
    }

    override fun resume() = Unit

    override fun release() = Unit

    fun finish() = onEnded()
}
