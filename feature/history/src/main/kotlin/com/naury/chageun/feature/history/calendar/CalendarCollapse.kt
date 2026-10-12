package com.naury.chageun.feature.history.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/**
 * 휴대폰 달력에서 아래 목록을 손가락으로 밀 때 달력을 접고 편다.
 * - 월 보기에서 위로 밀면 그만큼 먼저 가로채 주 보기로 접는다. 접힌 뒤부터는 목록이 그대로 움직인다.
 * - 주 보기에서 목록 맨 위에서 더 당기면(목록이 쓰고 남은 만큼) 월 보기로 펼친다.
 * 관성 스크롤로 맨 위에 닿았을 때는 펼치지 않는다. 손을 뗀 뒤 저절로 달력이 커지면 놀라기 때문이다.
 * 접근성 서비스나 테스트가 부르는 스크롤 동작은 nested scroll을 거치지 않아 모드를 바꾸지 않는다.
 */
internal class CalendarCollapseConnection(
    private val thresholdPx: Float,
    private val currentMode: () -> CalendarMode,
    private val onModeChange: (CalendarMode) -> Unit,
) : NestedScrollConnection {
    private var pending = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (currentMode() != CalendarMode.Month || available.y >= 0f || source != NestedScrollSource.UserInput) {
            return Offset.Zero
        }
        pending = minOf(pending, 0f) + available.y
        if (pending <= -thresholdPx) {
            pending = 0f
            onModeChange(CalendarMode.Week)
        }
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (currentMode() != CalendarMode.Week || available.y <= 0f || source != NestedScrollSource.UserInput) {
            return Offset.Zero
        }
        pending = maxOf(pending, 0f) + available.y
        if (pending >= thresholdPx) {
            pending = 0f
            onModeChange(CalendarMode.Month)
        }
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        pending = 0f
        return Velocity.Zero
    }
}

@Composable
internal fun rememberCalendarCollapse(
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
): NestedScrollConnection {
    val currentMode by rememberUpdatedState(mode)
    val currentOnModeChange by rememberUpdatedState(onModeChange)
    val threshold = with(LocalDensity.current) { COLLAPSE_THRESHOLD.toPx() }
    return remember(threshold) {
        CalendarCollapseConnection(threshold, { currentMode }, { currentOnModeChange(it) })
    }
}

private val COLLAPSE_THRESHOLD = 24.dp
