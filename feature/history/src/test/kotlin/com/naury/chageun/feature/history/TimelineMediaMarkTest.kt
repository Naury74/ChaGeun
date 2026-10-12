package com.naury.chageun.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 기록 목록 카드의 첨부 표시. 사진·영수증과 녹음한 소리를 따로 센다. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class TimelineMediaMarkTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun record(id: String, type: TimelineEventType, day: Int, title: String, cost: Long?) = TimelineItem(
        RecordRef(type, id),
        LocalDate.of(2026, 10, day),
        title,
        null,
        Kilometers(41_000L + day * 30),
        cost,
        RecordSource.User,
        Instant.EPOCH,
    )

    private val sections = listOf(
        TimelineSection(
            YearMonth.of(2026, 10),
            listOf(
                record("knock", TimelineEventType.Repair, 9, "Engine knock", 30_000).copy(soundCount = 2),
                record("bumper", TimelineEventType.Repair, 6, "Bumper", 120_000)
                    .copy(attachmentCount = 3, soundCount = 1),
                record("fuel", TimelineEventType.Fuel, 3, "S-Oil", 70_000).copy(attachmentCount = 1),
                record("note", TimelineEventType.Note, 1, "Car wash", 15_000),
            ),
            totalWon = 235_000,
        ),
    )

    @Composable
    private fun Timeline() {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false, sections = sections),
            isTwoPane = false,
            onKeywordChanged = {},
            onFilterSelected = {},
            onSelect = {},
            onDelete = {},
            onAdd = {},
            onAddPhotos = {},
            onDeleteAttachment = {},
            onDismissAttachFailure = {},
        )
    }

    @Test
    fun cards_readPhotoAndSoundCountsApart() {
        composeRule.setContent { ChageunTheme { Timeline() } }

        composeRule.onNodeWithContentDescription("2 sound recordings attached").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("3 photos attached").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1 sound recording attached").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1 photo attached").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("attached", substring = true).assertCountEquals(4)
    }

    @Test
    @Config(fontScale = 2f)
    fun largeFont_keepsTheMarksVisible() {
        composeRule.setContent { ChageunTheme { Timeline() } }

        composeRule.assertNoClippedText()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_marksKorean() {
        composeRule.setContent { AppFrame { Timeline() } }
        composeRule.captureScreen("history_media_marks_ko")
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h800dp-night-hdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_marksKoreanDark() {
        composeRule.setContent { AppFrame { Timeline() } }
        composeRule.captureScreen("history_media_marks_ko_dark")
    }
}
