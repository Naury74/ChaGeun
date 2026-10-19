package com.naury.chageun.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.naury.kinetica.ui.KChipGroup
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.kSelectable

/**
 * 선택형 입력의 기본 단위. 선택되면 대표 색으로 채워지고 체크가 그려지며 폭이 부드럽게 늘어나(Kinetica 칩)
 * 타이핑 없이 고른 값이 한눈에 보인다. TalkBack은 라디오 버튼으로 읽는다.
 * 여러 칩을 함께 놓을 때는 [ChoicePillGroup]이 한 묶음으로 읽혀 더 낫다.
 */
@Composable
fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // 칩 하나짜리 묶음으로 그려야 라디오 버튼 의미와 칩 모양을 함께 얻는다.
    KChipGroup(
        chips = listOf(label),
        selected = if (selected) setOf(0) else emptySet(),
        onSelectedChange = { onClick() },
        modifier = modifier,
        variant = KVariant.Outlined,
        minTouchTarget = true,
        shadowRoom = false,
    )
}

/**
 * 하나만 고르는 칩 묶음. [selectedIndex]가 null이면 아무것도 고르지 않은 상태다. 이미 고른 칩을 다시 눌러도
 * [onSelect]를 부른다(달력을 다시 여는 "다른 날"처럼).
 *
 * @param wrap false면 한 줄에 두고 가로로 넘기며, 끝을 흐려 더 있다는 것을 알린다. [contentPadding]은 이때만 쓴다.
 */
@Composable
fun ChoicePillGroup(
    labels: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    wrap: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    KChipGroup(
        chips = labels,
        selected = selectedIndex?.let(::setOf).orEmpty(),
        onSelectedChange = { next -> next.firstOrNull()?.let(onSelect) },
        modifier = modifier,
        variant = KVariant.Outlined,
        wrap = wrap,
        contentPadding = contentPadding,
        fadeEdges = !wrap,
        minTouchTarget = true,
        shadowRoom = false,
    )
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
        modifier = modifier.kSelectable(
            selected = selected,
            shape = MaterialTheme.shapes.medium,
            role = Role.RadioButton,
        ) {
            if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
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
