package com.naury.chageun.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.launchExternal

@Composable
fun AiHubRoute(focusItem: MaintenanceItem?, onBack: () -> Unit) {
    val viewModel = hiltViewModel<AiHubViewModel, AiHubViewModel.Factory>(key = "ai-${focusItem?.name}") {
        it.create(focusItem)
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
                    context.launchExternal { shareToAi(context, provider, prompt, chooserTitle) }
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(ChageunTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ai_back))
            }
            Text(
                stringResource(R.string.ai_title),
                style = MaterialTheme.typography.headlineSmall,
                color = ai.content,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            stringResource(R.string.ai_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.question,
            onValueChange = actions.onQuestionChanged,
            label = { Text(stringResource(R.string.ai_question_label)) },
            isError = uiState.isQuestionMissing,
            supportingText = if (uiState.isQuestionMissing) {
                (
                    {
                        Text(stringResource(R.string.ai_question_required))
                    }
                    )
            } else {
                null
            },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            suggestions(uiState.options.focusItem).forEach { suggestion ->
                AssistChip(
                    onClick = { actions.onQuestionChanged(suggestion) },
                    // 긴 추천 질문은 두 줄이 되는데, Chip 최소 높이 32dp로는 위아래 여백이 남지 않는다.
                    label = { Text(suggestion, modifier = Modifier.padding(vertical = ChageunTheme.spacing.xs)) },
                )
            }
        }
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
                ) { Text(provider.label ?: stringResource(R.string.ai_other_app)) }
            }
        }
        Text(
            stringResource(R.string.ai_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContextPreview(uiState: AiHubUiState, promptText: String?, actions: AiHubActions) {
    var isTextVisible by rememberSaveable { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            Text(stringResource(R.string.ai_preview_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.ai_included), style = MaterialTheme.typography.labelLarge)
            PreviewRow(Icons.Filled.Check, stringResource(R.string.ai_included_vehicle))
            PreviewRow(Icons.Filled.Check, stringResource(R.string.ai_included_mileage))
            PreviewRow(Icons.Filled.Check, stringResource(R.string.ai_included_maintenance))
            ToggleRow(
                stringResource(R.string.ai_include_records),
                uiState.options.includeRecords,
                actions.onIncludeRecordsChanged,
            )
            ToggleRow(
                stringResource(R.string.ai_include_costs),
                uiState.options.includeCosts && uiState.options.includeRecords,
                actions.onIncludeCostsChanged,
                enabled = uiState.options.includeRecords,
            )
            Text(stringResource(R.string.ai_excluded), style = MaterialTheme.typography.labelLarge)
            PreviewRow(Icons.Filled.Lock, stringResource(R.string.ai_excluded_plate))
            PreviewRow(Icons.Filled.Lock, stringResource(R.string.ai_excluded_owner))
            PreviewRow(Icons.Filled.Lock, stringResource(R.string.ai_excluded_location))
            if (promptText != null) {
                TextButton(onClick = { isTextVisible = !isTextVisible }) {
                    Text(stringResource(if (isTextVisible) R.string.ai_hide_text else R.string.ai_show_text))
                }
                if (isTextVisible) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
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
}

@Composable
private fun PreviewRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.widthIn(max = ICON_SIZE))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun suggestions(focusItem: MaintenanceItem?): List<String> = if (focusItem != null) {
    listOf(stringResource(R.string.ai_suggestion_item, stringResource(focusItem.labelRes)))
} else {
    listOf(
        stringResource(R.string.ai_suggestion_status),
        stringResource(R.string.ai_suggestion_oil),
        stringResource(R.string.ai_suggestion_trip),
        stringResource(R.string.ai_suggestion_cost),
    )
}

private val ICON_SIZE = 20.dp
