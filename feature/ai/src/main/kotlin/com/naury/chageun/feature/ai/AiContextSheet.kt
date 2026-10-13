package com.naury.chageun.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ai.SharedRecord
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.CardSubheader
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.labelRes

/**
 * 기획 9.3 보낼 정보 확인. 무엇이 함께 가고 무엇이 빠지는지, 실제로 보낼 글 전체를 보여 준다.
 * 기기 안 AI도 같은 글을 쓰므로 어느 AI를 고르든 같은 내용이다.
 */
@Composable
internal fun ContextPreview(
    uiState: AiHubUiState,
    promptText: String?,
    actions: AiHubActions,
    modifier: Modifier = Modifier,
) {
    var isTextVisible by rememberSaveable { mutableStateOf(false) }
    val good = ChageunTheme.colors.good
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ChageunTheme.spacing.gutter)
            .padding(bottom = ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
    ) {
        Text(
            stringResource(R.string.ai_preview_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        CardGroup(title = null) {
            CardSubheader(stringResource(R.string.ai_included))
            ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_vehicle), tone = good)
            ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_mileage), tone = good)
            ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_maintenance), tone = good)
            // 기록 상세에서 열었으면 그 기록이 함께 간다는 것을 분명히 보여 준다.
            if (uiState.facts?.focusRecord != null || uiState.options.focusRecord != null) {
                ListRow(
                    Icons.Filled.Check,
                    stringResource(R.string.ai_included_focus_record),
                    body = stringResource(R.string.ai_included_focus_record_body),
                    tone = good,
                )
            }
            GroupDivider()
            ToggleListRow(
                icon = Icons.Filled.History,
                title = stringResource(R.string.ai_include_records),
                body = null,
                checked = uiState.options.includeRecords,
                onCheckedChange = actions.onIncludeRecordsChanged,
            )
            ToggleListRow(
                icon = Icons.Filled.Payments,
                title = stringResource(R.string.ai_include_costs),
                body = stringResource(R.string.ai_include_costs_body),
                checked = uiState.options.includeCosts && uiState.options.includeRecords,
                onCheckedChange = actions.onIncludeCostsChanged,
                enabled = uiState.options.includeRecords,
            )
            GroupDivider()
            CardSubheader(stringResource(R.string.ai_excluded))
            val critical = ChageunTheme.colors.critical
            ListRow(Icons.Filled.Lock, stringResource(R.string.ai_excluded_plate), tone = critical)
            ListRow(Icons.Filled.Lock, stringResource(R.string.ai_excluded_owner), tone = critical)
            ListRow(Icons.Filled.Lock, stringResource(R.string.ai_excluded_location), tone = critical)
        }
        if (promptText != null) {
            TextButton(onClick = { isTextVisible = !isTextVisible }) {
                Text(stringResource(if (isTextVisible) R.string.ai_hide_text else R.string.ai_show_text))
            }
            if (isTextVisible) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        promptText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(ChageunTheme.spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
internal fun suggestions(focusItem: MaintenanceItem?, focusRecord: SharedRecord?): List<String> = when {
    focusRecord != null -> {
        val recordItem = focusRecord.maintenanceItem
        listOf(
            stringResource(R.string.ai_suggestion_record_summary),
            when {
                recordItem != null -> stringResource(
                    R.string.ai_suggestion_record_service,
                    stringResource(recordItem.labelRes),
                )
                focusRecord.type == TimelineEventType.Fuel -> stringResource(R.string.ai_suggestion_record_fuel)
                else -> stringResource(R.string.ai_suggestion_record_check)
            },
        )
    }
    focusItem != null -> listOf(
        stringResource(R.string.ai_suggestion_item, stringResource(focusItem.labelRes)),
        stringResource(R.string.ai_suggestion_status),
    )
    else ->
        listOf(
            stringResource(R.string.ai_suggestion_status),
            stringResource(R.string.ai_suggestion_oil),
            stringResource(R.string.ai_suggestion_trip),
            stringResource(R.string.ai_suggestion_cost),
        )
}
