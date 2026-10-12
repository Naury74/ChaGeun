package com.naury.chageun.feature.history.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunSpacing
import com.naury.chageun.core.designsystem.theme.Gutters
import com.naury.chageun.core.model.HistoryView
import com.naury.chageun.core.ui.Hinge
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import com.naury.chageun.feature.history.HistoryScreen
import com.naury.chageun.feature.history.HistoryUiState
import com.naury.chageun.feature.history.HistoryViewToggle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 달력 보기. 고정된 날짜(2026-10-12)와 데이터로 그려 매번 같은 그림이 나온다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryCalendarScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** 앱의 기록 탭처럼 보기 전환을 위에 두고 그린다. */
    private fun capture(
        name: String,
        isTwoPane: Boolean = false,
        isThreePane: Boolean = false,
        spacing: ChageunSpacing = Gutters.Compact,
    ) {
        val state = CalendarFixtures.state(photoPaths = CalendarFixtures.photoFiles())
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
                    HistoryScreen(
                        uiState = HistoryUiState(isLoading = false),
                        isTwoPane = isTwoPane,
                        isThreePane = isThreePane,
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
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        waitForPhotos()
        composeRule.captureScreen(name)
    }

    /** 접힌 자세는 Hinge를 직접 넘겨 그린다. */
    private fun captureWithHinge(name: String, hinge: Hinge) {
        val state = CalendarFixtures.state(photoPaths = CalendarFixtures.photoFiles())
        composeRule.setContent {
            AppFrame(Gutters.Medium) {
                HistoryCalendar(
                    state = state,
                    actions = CalendarActions(),
                    detail = null,
                    detailPane = { _, modifier -> Spacer(modifier) },
                    onCloseDetail = {},
                    isTwoPane = hinge.isVertical,
                    isThreePane = false,
                    modifier = Modifier.fillMaxSize(),
                    hinge = hinge,
                )
            }
        }
        waitForPhotos()
        composeRule.captureScreen(name)
    }

    // 사진은 IO 스레드에서 디코딩되므로 붙을 때까지 잠깐 기다린 뒤 다시 그린다.
    private fun waitForPhotos() {
        composeRule.waitForIdle()
        Thread.sleep(PHOTO_DECODE_MS)
        composeRule.waitForIdle()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun phoneKorean() = capture("history_calendar_phone_ko")

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun phoneDark() = capture("history_calendar_phone_dark")

    /** 840dp: 왼쪽 달력, 오른쪽 고른 날 목록. */
    @Test
    @Config(qualifiers = ScreenshotDevices.EXPANDED_KO)
    fun expandedTwoPaneKorean() = capture("history_calendar_expanded_ko", isTwoPane = true, spacing = Gutters.Expanded)

    /** 책 자세: 세로 접힘 양쪽에 달력과 목록을 나눠 둔다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.FOLD_KO)
    fun foldBookPostureKorean() =
        captureWithHinge("history_calendar_fold_ko", Hinge(start = FOLD_HINGE_X, end = FOLD_HINGE_X, isVertical = true))

    /** 탁자 자세: 위 칸 달력, 아래 칸 목록. */
    @Test
    @Config(qualifiers = ScreenshotDevices.FOLD_KO)
    fun foldTabletopKorean() = captureWithHinge(
        "history_calendar_tabletop_ko",
        Hinge(start = TABLETOP_HINGE_Y, end = TABLETOP_HINGE_Y, isVertical = false),
    )

    /** 1600dp 이상: 레이어·월 요약 칸, 달력, 목록. */
    @Test
    @Config(qualifiers = "ko-w1700dp-h1000dp-hdpi")
    fun extraLargeThreePaneKorean() =
        capture("history_calendar_xlarge_ko", isTwoPane = true, isThreePane = true, spacing = Gutters.Expanded)

    private companion object {
        const val PHOTO_DECODE_MS = 300L

        // FOLD_KO(792dp, hdpi)의 가운데. Hinge는 창 픽셀 좌표다.
        const val FOLD_HINGE_X = 594f
        const val TABLETOP_HINGE_Y = 615f
    }
}
