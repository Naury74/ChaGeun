package com.naury.chageun.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme

/**
 * iOS의 세그먼트 컨트롤. 회색 트랙 위에서 선택 막대가 스프링으로 미끄러진다.
 * 선택지가 2~4개이고 모두 한눈에 보여야 할 때 쓴다. 더 많으면 칩 목록을 쓴다.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ChageunTheme.spacing.minTouchTarget)
            .background(colors.surfaceVariant, TRACK_SHAPE)
            .padding(TRACK_PADDING),
    ) {
        val segmentWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(segmentWidth * selectedIndex, motionSpec(), label = "segment-thumb")
        // 큰 글꼴에서 글자가 두 줄이 되면 높이가 늘어나므로 선택 막대는 실제 높이를 따른다.
        Box(Modifier.matchParentSize()) {
            Surface(
                shape = THUMB_SHAPE,
                color = colors.surfaceBright,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(segmentWidth)
                    .fillMaxHeight(),
            ) {}
        }
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget - TRACK_PADDING * 2)
                        .selectable(selected = selected, role = Role.RadioButton) {
                            if (!selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(index)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

private val TRACK_PADDING = 2.dp
private val TRACK_SHAPE = RoundedCornerShape(10.dp)
private val THUMB_SHAPE = RoundedCornerShape(8.dp)
