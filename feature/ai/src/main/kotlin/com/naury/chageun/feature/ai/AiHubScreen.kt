package com.naury.chageun.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ai.SharedRecord
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.CardSubheader
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.Hinge
import com.naury.chageun.core.ui.HingeAwarePanes
import com.naury.chageun.core.ui.ItemIconBadge
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.currentSeparatingHinge
import com.naury.chageun.core.ui.isListDetailThreePane
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
    var installedProviders by remember { mutableStateOf<Set<AiProvider>?>(null) }
    // 다른 화면에서 AI 앱을 설치하고 돌아와도 바로 반영되도록 돌아올 때마다 다시 확인한다.
    LifecycleResumeEffect(context) {
        installedProviders = installedAiProviders(context)
        onPauseOrDispose {}
    }
    AiHubScreen(
        uiState = uiState,
        promptText = prompt,
        installedProviders = installedProviders,
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

/**
 * Expanded 폭부터 질문과 보낼 정보를 나란히, Extra Large 폭부터는 보낼 곳까지 세 칸으로 나눈다.
 * Large 폭은 왼쪽 고정 메뉴가 자리를 차지해 세 칸이면 칸마다 너무 좁다.
 */
fun aiPaneCount(windowSizeClass: WindowSizeClass): Int = when {
    isListDetailThreePane(windowSizeClass) -> THREE_PANES
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> TWO_PANES
    else -> SINGLE_PANE
}

@Composable
private fun currentAiPaneCount(hinge: Hinge?): Int {
    val count = aiPaneCount(currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass)
    // Book 자세: 한 칸짜리 화면이 접히는 부분을 가로지르지 않게 한다.
    return if (hinge?.isVertical == true) maxOf(count, TWO_PANES) else count
}

/**
 * [installedProviders]는 기기에 설치된 AI 앱이다. 세 칸일 때 보낼 곳마다 앱이 바로 열리는지 알려 주는 데 쓴다.
 * null이면 아직 모르므로 상태를 표시하지 않는다.
 */
@Composable
fun AiHubScreen(
    uiState: AiHubUiState,
    promptText: String?,
    actions: AiHubActions,
    modifier: Modifier = Modifier,
    installedProviders: Set<AiProvider>? = null,
    hinge: Hinge? = currentSeparatingHinge(),
    paneCount: Int = currentAiPaneCount(hinge),
) {
    LargeTitleScaffold(
        title = stringResource(R.string.ai_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ai_back))
            }
        },
    ) { padding ->
        val isTabletop = hinge != null && !hinge.isVertical
        if (paneCount == SINGLE_PANE && !isTabletop) {
            SinglePane(uiState, promptText, actions, Modifier.padding(padding))
        } else {
            AiPanes(
                content = AiPaneContent(uiState, promptText, actions, installedProviders),
                paneCount = if (isTabletop) TWO_PANES else paneCount,
                stacked = isTabletop,
                hinge = hinge,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun SinglePane(uiState: AiHubUiState, promptText: String?, actions: AiHubActions, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ChageunTheme.spacing.gutter)
            .padding(bottom = ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
    ) {
        AiIntro()
        QuestionSection(uiState, actions)
        ContextPreview(uiState, promptText, actions)
        ProviderButtons(promptText, actions)
        Disclaimer()
    }
}

private data class AiPaneContent(
    val uiState: AiHubUiState,
    val promptText: String?,
    val actions: AiHubActions,
    val installedProviders: Set<AiProvider>?,
)

/**
 * 두 칸: 질문과 보낼 곳 | 보낼 정보. 세 칸: 질문 | 보낼 정보 | 보낼 곳과 안내.
 * Tabletop 자세에서는 두 칸을 위아래로 쌓아 접히는 부분 위아래로 나눈다.
 * 칸마다 따로 스크롤하고, 넓은 창에서도 한 칸의 글 폭이 너무 길어지지 않게 가운데에 모은다.
 */
@Composable
private fun AiPanes(content: AiPaneContent, paneCount: Int, stacked: Boolean, hinge: Hinge?, modifier: Modifier) {
    val (uiState, promptText, actions) = content
    HingeAwarePanes(
        weights = List(paneCount) { 1f },
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = ChageunTheme.spacing.gutter),
        stacked = stacked,
        hinge = hinge,
    ) {
        AiPane(Modifier.testTag(AiHubTags.QUESTION_PANE)) {
            AiIntro()
            QuestionSection(uiState, actions)
            if (paneCount == TWO_PANES) {
                ProviderButtons(promptText, actions)
                Disclaimer()
            }
        }
        AiPane(Modifier.testTag(AiHubTags.CONTEXT_PANE)) {
            // 옆 칸에 여유가 있으니 실제로 보낼 글을 처음부터 펼쳐 둔다.
            ContextPreview(uiState, promptText, actions, initiallyShowText = true)
        }
        if (paneCount == THREE_PANES) {
            AiPane(Modifier.testTag(AiHubTags.PROVIDER_PANE)) {
                ProviderList(promptText, content.installedProviders, actions)
                Disclaimer()
            }
        }
    }
}

@Composable
private fun AiPane(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = PANE_MAX_WIDTH)
                .fillMaxWidth()
                .padding(bottom = ChageunTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
            content = content,
        )
    }
}

@Composable
private fun AiIntro() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        ItemIconBadge(Icons.Filled.AutoAwesome, ChageunTheme.colors.ai, size = 44.dp)
        Text(
            stringResource(R.string.ai_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuestionSection(uiState: AiHubUiState, actions: AiHubActions) {
    val ai = ChageunTheme.colors.ai
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderButtons(promptText: String?, actions: AiHubActions) {
    val ai = ChageunTheme.colors.ai
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
}

/** 세 칸일 때 보낼 곳 칸. 버튼 대신 목록으로 두고, 앱이 바로 열리는지 공유 시트를 거치는지 함께 알려 준다. */
@Composable
private fun ProviderList(promptText: String?, installedProviders: Set<AiProvider>?, actions: AiHubActions) {
    val ai = ChageunTheme.colors.ai
    CardGroup(stringResource(R.string.ai_send_with)) {
        AiProvider.entries.forEachIndexed { index, provider ->
            if (index > 0) GroupDivider()
            ListRow(
                icon = Icons.AutoMirrored.Filled.Send,
                title = provider.label ?: stringResource(R.string.ai_other_app),
                body = installedProviders?.let { stringResource(providerStatusRes(provider, it)) },
                tone = ai,
                onClick = { actions.onShare(provider) },
                enabled = promptText != null,
            )
        }
    }
}

private fun providerStatusRes(provider: AiProvider, installed: Set<AiProvider>): Int = when {
    provider.packageName == null -> R.string.ai_provider_other
    provider in installed -> R.string.ai_provider_installed
    else -> R.string.ai_provider_missing
}

@Composable
private fun Disclaimer() {
    Text(
        stringResource(R.string.ai_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ContextPreview(
    uiState: AiHubUiState,
    promptText: String?,
    actions: AiHubActions,
    initiallyShowText: Boolean = false,
) {
    // 칸 수가 바뀌면 그 배치의 기본값으로 다시 시작한다.
    var isTextVisible by rememberSaveable(initiallyShowText) { mutableStateOf(initiallyShowText) }
    val good = ChageunTheme.colors.good
    CardGroup(stringResource(R.string.ai_preview_title)) {
        CardSubheader(stringResource(R.string.ai_included))
        ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_vehicle), tone = good)
        ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_mileage), tone = good)
        ListRow(Icons.Filled.Check, stringResource(R.string.ai_included_maintenance), tone = good)
        // 기록 상세에서 열었으면 그 기록이 함께 간다는 것을 보낼 정보에 분명히 보여 준다.
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

/** 칸을 찾는 테스트용 태그. */
object AiHubTags {
    const val QUESTION_PANE = "ai_question_pane"
    const val CONTEXT_PANE = "ai_context_pane"
    const val PROVIDER_PANE = "ai_provider_pane"
}

private const val SINGLE_PANE = 1
private const val TWO_PANES = 2
private const val THREE_PANES = 3

private val ICON_SIZE = 20.dp
private val PANE_MAX_WIDTH = 600.dp
