package com.naury.chageun.feature.history.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.FormHeader
import com.naury.chageun.core.ui.MaintenanceItemIcon
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.feature.history.ButtonPair
import com.naury.chageun.feature.history.R
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KButtonSize
import com.naury.kinetica.ui.KCheckbox
import com.naury.kinetica.ui.KVariant

/**
 * 달력에 보일 정비 항목을 여러 개 고르는 시트. 고르는 동안에는 달력을 바꾸지 않고 [적용]을 눌러야 반영한다.
 * 회전이나 접기 뒤에도 고르던 항목이 남도록 초안을 저장 가능한 상태로 둔다.
 */
@Composable
internal fun CalendarItemSheet(
    selected: Set<MaintenanceItem>,
    isExpanded: Boolean,
    onApply: (Set<MaintenanceItem>) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(selected.map { it.name }) }
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(ChageunTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            FormHeader(Icons.Filled.Tune, ChageunTheme.colors.unknown, stringResource(R.string.calendar_items_title))
            MaintenanceItem.entries.forEach { item ->
                ItemRow(item, checked = item.name in draft) { checked ->
                    draft = if (checked) draft + item.name else draft - item.name
                }
            }
            ButtonPair {
                KButton(
                    stringResource(R.string.history_filter_reset),
                    onClick = { draft = emptyList() },
                    variant = KVariant.Text,
                    size = KButtonSize.Large,
                    fullWidth = true,
                )
                KButton(
                    stringResource(R.string.history_filter_apply),
                    onClick = {
                        onApply(MaintenanceItem.entries.filter { it.name in draft }.toSet())
                        onDismiss()
                    },
                    size = KButtonSize.Large,
                    fullWidth = true,
                )
            }
        }
    }
}

/** 행 전체가 체크박스다. TalkBack에서는 항목 이름과 체크 상태를 한 번에 읽는다. */
@Composable
private fun ItemRow(item: MaintenanceItem, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ChageunTheme.spacing.minTouchTarget)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        MaintenanceItemIcon(item, size = ITEM_ICON_SIZE)
        Text(
            stringResource(item.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        KCheckbox(checked = checked, onCheckedChange = null)
    }
}

private val ITEM_ICON_SIZE = 32.dp
