package com.naury.chageun.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme

/** 선택형 입력의 기본 단위. 선택되면 Primary로 채워져 타이핑 없이 고른 값이 한눈에 보인다. */
@Composable
fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val container by animateColorAsState(if (selected) colors.primary else colors.surface, motionSpec(), "pill-bg")
    val content by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface, motionSpec(), "pill-fg")
    Surface(
        modifier = modifier
            .heightIn(min = ChageunTheme.spacing.minTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }),
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 10.dp))
        }
    }
}

/** 아이콘과 이름이 있는 큰 선택 카드. 연료처럼 항목 수가 적고 그림으로 구분되는 선택지에 쓴다. */
@Composable
fun ChoiceCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val container by animateColorAsState(
        if (selected) colors.primaryContainer else colors.surface,
        motionSpec(),
        "card-bg",
    )
    Surface(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = {
            if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        }),
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            )
        }
    }
}
