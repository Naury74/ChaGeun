package com.naury.chageun.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ai.SharedRecord
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.CardSubheader
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ItemIconBadge
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.launchExternal

@Composable
fun AiHubRoute(focusItem: MaintenanceItem?, onBack: () -> Unit, focusRecord: RecordRef? = null) {
    val key = "ai-${focusItem?.name}-${focusRecord?.let { "${it.type}:${it.id}" }}"
    val viewModel = hiltViewModel<AiHubViewModel, AiHubViewModel.Factory>(key = key) {
        it.create(focusItem, focusRecord)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.ai_chooser_title)
    val prompt = uiState.facts?.let { aiPromptText(it, uiState.question) }
    AiHubScreen(
        uiState = uiState,
        promptText = prompt,
        actions = AiHubActions(
            onBack = onBack,
            onQuestionChanged = viewModel::onQuestionChanged,
            onIncludeRecordsChanged = viewModel::onIncludeRecordsChanged,
            onIncludeCostsChanged = viewModel::onIncludeCostsChanged,
            onShare = { provider ->
                if (viewModel.validateBeforeShare() &&
                    prompt != null
                ) {
                    context.launchExternal {
                        shareToAi(context, provider, prompt, chooserTitle)
                        viewModel.onShared(provider)
                    }
                }
            },
        ),
    )
}

data class AiHubActions(
    val onBack: () -> Unit,
    val onQuestionChanged: (String) -> Unit,
    val onIncludeRecordsChanged: (Boolean) -> Unit,
    val onIncludeCostsChanged: (Boolean) -> Unit,
    val onShare: (AiProvider) -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiHubScreen(uiState: AiHubUiState, promptText: String?, actions: AiHubActions, modifier: Modifier = Modifier) {
    val ai = ChageunTheme.colors.ai
    LargeTitleScaffold(
        title = stringResource(R.string.ai_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ai_back))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ChageunTheme.spacing.gutter)
                .padding(bottom = ChageunTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                ItemIconBadge(Icons.Filled.AutoAwesome, ai, size = 44.dp)
                Text(
                    stringResource(R.string.ai_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            CardGroup(stringResource(R.string.ai_pick_question)) {
                suggestions(uiState.options.focusItem, uiState.facts?.focusRecord).forEachIndexed { index, suggestion ->
                    if (index > 0) GroupDivider()
                    val isSelected = uiState.question == suggestion
                    ListRow(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = suggestion,
                        tone = if (isSelected) ai else ChageunTheme.colors.unknown,
                        onClick = { actions.onQuestionChanged(suggestion) },
                        trailing = {
                            if (isSelected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ai.content)
                        },
                    )
                }
            }
            OutlinedTextField(
                value = uiState.question,
                onValueChange = actions.onQuestionChanged,
                label = { Text(stringResource(R.string.ai_question_label)) },
                isError = uiState.isQuestionMissing,
                supportingText = stringResource(R.string.ai_question_required)
                    .takeIf { uiState.isQuestionMissing }
                    ?.let { { Text(it) } },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            ContextPreview(uiState, promptText, actions)
            Text(stringResource(R.string.ai_send_with), style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                AiProvider.entries.forEach { provider ->
                    FilledTonalButton(
                        onClick = { actions.onShare(provider) },
                        enabled = promptText != null,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ai.container,
                            contentColor = ai.content,
                        ),
                        modifier = Modifier.heightIn(min = ChageunTheme.spacing.minTouchTarget),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(ICON_SIZE),
                        )
                        Spacer(Modifier.width(ChageunTheme.spacing.xs))
                        Text(provider.label ?: stringResource(R.string.ai_other_app))
                    }
                }
            }
            Text(
                stringResource(R.string.ai_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContextPreview(uiState: AiHubUiState, promptText: String?, actions: AiHubActions) {
    var isTextVisible by rememberSaveable { mutableStateOf(false) }
    val good = ChageunTheme.colors.good
    CardGroup(stringResource(R.string.ai_preview_title)) {
        CardSubheader(stringResource(R.string.ai_included))
        ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_vehicle), tone = good)
        ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_mileage), tone = good)
        ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_maintenance), tone = good)
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
            body = null,
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
        if (promptText != null) {
            GroupDivider()
            TextButton(
                onClick = { isTextVisible = !isTextVisible },
                modifier = Modifier.padding(horizontal = ChageunTheme.spacing.xs),
            ) {
                Text(stringResource(if (isTextVisible) R.string.ai_hide_text else R.string.ai_show_text))
            }
            if (isTextVisible) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(
                        start = ChageunTheme.spacing.md,
                        end = ChageunTheme.spacing.md,
                        bottom = ChageunTheme.spacing.md,
                    ),
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
private fun suggestions(focusItem: MaintenanceItem?, focusRecord: SharedRecord?): List<String> = when {
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
    focusItem != null -> listOf(stringResource(R.string.ai_suggestion_item, stringResource(focusItem.labelRes)))
    else ->
        listOf(
            stringResource(R.string.ai_suggestion_status),
            stringResource(R.string.ai_suggestion_oil),
            stringResource(R.string.ai_suggestion_trip),
            stringResource(R.string.ai_suggestion_cost),
        )
}

private val ICON_SIZE = 20.dp
