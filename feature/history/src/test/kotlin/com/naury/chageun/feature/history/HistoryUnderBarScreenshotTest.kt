package com.naury.chageun.feature.history

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.HistoryView
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Instant
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 휴대폰 기록 목록이 위 막대 뒤로 지나가고, 달 머리글은 막대 바로 아래에 붙는지 본다. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = ScreenshotDevices.PHONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryUnderBarScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sections = listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 8), YearMonth.of(2026, 7)).map { month ->
        TimelineSection(
            month,
            (1..ROWS_PER_MONTH).map { day ->
                TimelineItem(
                    RecordRef(TimelineEventType.Fuel, "$month-$day"),
                    month.atDay(day * 2),
                    "S-Oil",
                    null,
                    Kilometers(40_000L + day * 100),
                    60_000L + day * 1_000,
                    RecordSource.User,
                    Instant.EPOCH,
                )
            },
        )
    }

    private fun show() = composeRule.setContent {
        AppFrame {
            LargeTitleScaffold(title = "History") { padding ->
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
                    contentTop = padding.calculateTopPadding(),
                    listHeader = {
                        HistoryViewToggle(
                            HistoryView.List,
                            onChange = {},
                            Modifier.padding(horizontal = ChageunTheme.spacing.gutter).widthIn(max = 360.dp),
                        )
                    },
                )
            }
        }
    }

    @Test
    fun expanded() {
        show()
        composeRule.captureScreen("history_under_bar_expanded")
    }

    @Test
    fun scrolled() {
        show()
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.captureScreen("history_under_bar_scrolled")
    }

    private companion object {
        const val ROWS_PER_MONTH = 6
    }
}
