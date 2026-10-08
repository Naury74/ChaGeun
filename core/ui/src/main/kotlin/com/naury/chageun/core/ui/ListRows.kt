package com.naury.chageun.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors

/** 제목 아래 한 장의 카드로 묶는 목록. 행 사이 구분선은 [GroupDivider]로 넣는다. */
@Composable
fun CardGroup(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        title?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(top = ChageunTheme.spacing.xs)
                    .semantics { heading() },
            )
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = content)
        }
    }
}

/** 카드 안에서 행 묶음을 나누는 작은 제목. */
@Composable
fun CardSubheader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            start = ChageunTheme.spacing.md,
            end = ChageunTheme.spacing.md,
            top = ChageunTheme.spacing.sm,
        ),
    )
}

/** 아이콘 너비만큼 들여 써서 아이콘 열이 끊기지 않게 보이는 구분선. */
@Composable
fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = ROW_TEXT_START),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA),
    )
}

/**
 * 아이콘·제목·설명·끝 요소로 된 한 줄. [onClick]이 있으면 행 전체를 버튼으로 만들고
 * 끝 요소가 없을 때는 이동한다는 뜻으로 화살표를 붙인다.
 */
@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    tone: ToneColors = ChageunTheme.colors.unknown,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .pressable(onClick = onClick, style = PressStyle.Highlight, enabled = enabled)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
    } else {
        Modifier
    }
    RowLayout(icon, title, body, tone, titleColor, modifier.then(clickModifier)) {
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 여러 개 중 하나를 고르는 행. 세 칸 배치의 필터 Pane처럼 고른 행을 계속 강조해 둬야 하는 곳에 쓴다.
 * [trailingText]에는 개수처럼 짧은 값을 둔다.
 */
@Composable
fun SelectableListRow(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ToneColors = ChageunTheme.colors.unknown,
    trailingText: String? = null,
) {
    ListRow(
        icon = icon,
        title = title,
        tone = tone,
        onClick = onClick,
        modifier = modifier
            .semantics { this.selected = selected }
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
        trailing = {
            trailingText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/** 행 전체가 스위치다. TalkBack에서는 제목·설명·켜짐 상태를 한 번에 읽는다. */
@Composable
fun ToggleListRow(
    icon: ImageVector,
    title: String,
    body: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tone: ToneColors = ChageunTheme.colors.unknown,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    RowLayout(
        icon,
        title,
        body,
        tone,
        Color.Unspecified,
        modifier
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = {
                haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(it)
            })
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** 이름과 값 한 쌍. 값이 없으면 행을 그리지 않는다. */
@Composable
fun InfoListRow(icon: ImageVector, label: String, value: String?, modifier: Modifier = Modifier) {
    if (value.isNullOrBlank()) return
    RowLayout(icon, label, null, ChageunTheme.colors.unknown, MaterialTheme.colorScheme.onSurfaceVariant, modifier) {
        Text(value, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
    }
}

/**
 * 이름을 작게 위에, 값을 크게 아래에 둔 행. 값이 길어 한 줄 표에서 이름이 눌리는 상세 화면에 쓴다.
 * 값이 없으면 행을 그리지 않는다.
 */
@Composable
fun LabeledListRow(icon: ImageVector, label: String, value: String?, modifier: Modifier = Modifier) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIconBadge(icon, ChageunTheme.colors.unknown, size = ROW_ICON_SIZE)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RowLayout(
    icon: ImageVector,
    title: String,
    body: String?,
    tone: ToneColors,
    titleColor: Color,
    modifier: Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIconBadge(icon, tone, size = ROW_ICON_SIZE)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            body?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

private val ROW_ICON_SIZE = 36.dp
private val ROW_MIN_HEIGHT = 56.dp

// 행 좌우 여백(16) + 아이콘(36) + 간격(12)
private val ROW_TEXT_START = 64.dp
private const val DIVIDER_ALPHA = 0.6f
private const val DISABLED_ALPHA = 0.38f
