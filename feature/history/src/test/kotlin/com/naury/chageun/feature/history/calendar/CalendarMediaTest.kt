package com.naury.chageun.feature.history.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.Gutters
import com.naury.chageun.core.model.AlbumMediaType
import com.naury.chageun.core.model.HistoryView
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import com.naury.chageun.feature.history.HistoryScreen
import com.naury.chageun.feature.history.HistoryUiState
import com.naury.chageun.feature.history.HistoryViewToggle
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 달력의 동영상·소리 표시. 기본 고정 데이터의 3일에 앨범 동영상 하나와, 녹음한 소리 두 개가 붙은 수리 기록을 더한다.
 * 기존 달력 스크린샷이 바뀌지 않도록 이 테스트에서만 데이터를 덧붙인다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarMediaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val oct3: LocalDate = LocalDate.of(2026, 10, 3)

    private fun mediaState(photoPaths: Map<String, String> = emptyMap(), mode: CalendarMode? = null): CalendarUiState {
        val base = CalendarFixtures.state(selected = oct3, photoPaths = photoPaths, mode = mode)
        val month = base.calendar ?: error("fixture has a month")
        val day = month.day(oct3)
        val clip = CalendarFixtures.photo("clip", oct3, photoPaths["today"] ?: "/missing/clip.jpg")
            .copy(mediaType = AlbumMediaType.Video, durationMs = 42_000)
        val knock = CalendarFixtures.record("knock", TimelineEventType.Repair, oct3, "Engine knock", cost = 30_000)
            .copy(soundCount = 2)
        val withMedia = day.copy(photos = listOf(clip) + day.photos, records = day.records + knock)
        val withCounts = month.copy(
            days = month.days + (oct3 to withMedia),
            monthTotalWon = month.monthTotalWon + 30_000,
            recordCount = month.recordCount + 1,
            photoCount = month.photoCount + 1,
        )
        return base.copy(calendar = withCounts)
    }

    private fun show(state: CalendarUiState) = composeRule.setContent {
        ChageunTheme { calendarScreen(state) }
    }

    @Composable
    private fun calendarScreen(state: CalendarUiState, modifier: Modifier = Modifier) {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false),
            isTwoPane = false,
            onKeywordChanged = {},
            onFilterSelected = {},
            onSelect = {},
            onDelete = {},
            onAdd = {},
            onAddPhotos = {},
            onDeleteAttachment = {},
            onDismissAttachFailure = {},
            view = HistoryView.Calendar,
            calendar = HistoryCalendarSlot(state, CalendarActions()),
            modifier = modifier,
        )
    }

    private fun scrollToDescription(description: String) {
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasContentDescription(description, substring = true))
    }

    @Test
    fun dayCell_readsPhotosVideosAndSoundsApart() {
        show(mediaState())

        composeRule.onNode(
            hasContentDescription("Saturday, October 3, 3 records", substring = true) and
                hasContentDescription("1 photo, 1 video, 2 sound recordings", substring = true),
        ).assertExists()
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h2000dp")
    fun dayCell_readsInKorean() {
        show(mediaState())

        composeRule.onNode(hasContentDescription("사진 1장, 동영상 1개, 소리 2개", substring = true)).assertExists()
    }

    @Test
    fun monthSummary_countsPhotosAndVideosApart() {
        show(mediaState())

        composeRule.onNodeWithText("₩341,000 spent · 11 records · 4 photos · 1 video").assertIsDisplayed()
    }

    @Test
    fun monthSummary_photosChip_keepsBothPhotosAndVideos() {
        show(mediaState().copy(filter = CalendarFilter(kinds = setOf(CalendarKind.Photos))))

        composeRule.onNodeWithText("4 photos · 1 video", substring = true).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h2000dp")
    fun monthSummary_readsInKorean() {
        show(mediaState())

        composeRule.onNodeWithText("사진 4장 · 동영상 1개", substring = true).assertIsDisplayed()
    }

    @Test
    fun agenda_marksTheVideo_andTheRecordWithSounds() {
        show(mediaState())

        scrollToDescription("Video from")
        composeRule.onNodeWithContentDescription("Video from", substring = true).assertHasClickAction()
        scrollToDescription("2 sound recordings attached")
        composeRule.onNode(hasContentDescription("2 sound recordings attached", substring = true)).assertExists()
    }

    @Test
    @Config(fontScale = 2f)
    fun largeFont_keepsTheMarksVisible() {
        show(mediaState())

        composeRule.assertNoClippedText()
    }

    private fun capture(name: String) {
        // 주 보기는 칸이 커서 재생 표시가 잘 보이고, 아래에 그날 목록(동영상·소리 표시)이 함께 나온다.
        val state = mediaState(CalendarFixtures.photoFiles(), mode = CalendarMode.Week)
        val spacing = Gutters.Compact
        composeRule.setContent {
            AppFrame(spacing) {
                Column {
                    HistoryViewToggle(
                        view = HistoryView.Calendar,
                        onChange = {},
                        modifier = Modifier
                            .padding(horizontal = spacing.gutter, vertical = spacing.xxs)
                            .widthIn(max = 360.dp),
                    )
                    calendarScreen(state, Modifier.weight(1f))
                }
            }
        }
        // 사진은 IO 스레드에서 디코딩되므로 붙을 때까지 잠깐 기다린 뒤 다시 그린다.
        composeRule.waitForIdle()
        Thread.sleep(PHOTO_DECODE_MS)
        composeRule.waitForIdle()
        composeRule.captureScreen(name)
    }

    // 그날 목록의 사진 줄까지 담으려고 폰 폭에 세로만 길게 찍는다.
    @Test
    @Config(qualifiers = "ko-w360dp-h1100dp-hdpi")
    fun screenshot_videoAndSoundKorean() = capture("history_calendar_media_ko")

    @Test
    @Config(qualifiers = "ko-w360dp-h1100dp-night-hdpi")
    fun screenshot_videoAndSoundKoreanDark() = capture("history_calendar_media_ko_dark")

    private companion object {
        const val PHOTO_DECODE_MS = 300L
    }
}
