package com.naury.chageun.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.isExpandedWidth
import com.naury.chageun.core.ui.launchExternal

@Composable
fun AiHubRoute(focusItem: MaintenanceItem?, onBack: () -> Unit, focusRecord: RecordRef? = null) {
    val key = "ai-${focusItem?.name}-${focusRecord?.let { "${it.type}:${it.id}" }}"
    val viewModel = hiltViewModel<AiHubViewModel, AiHubViewModel.Factory>(key = key) {
        it.create(focusItem, focusRecord)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prompt = uiState.facts?.let { aiPromptText(it, uiState.question) }
    val instruction = stringResource(R.string.ai_on_device_instruction)
    val reminder = stringResource(R.string.ai_on_device_reminder)
    val historyLabel = stringResource(R.string.ai_history_label)
    val destinationName = destinationTitle(uiState.destination)
    val texts = ExternalTexts(
        chooserTitle = stringResource(R.string.ai_chooser_title),
        clipLabel = stringResource(R.string.ai_clip_label),
        copied = stringResource(R.string.ai_copied, destinationName),
    )
    val send = {
        when {
            prompt == null -> Unit
            // 작은 모델은 시스템 지시보다 질문 바로 뒤의 당부를 더 잘 따른다.
            uiState.destination == AiDestination.OnDevice ->
                viewModel.ask("$prompt\n\n$reminder", instruction, historyLabel)
            viewModel.validateBeforeShare() -> context.launchExternal {
                openExternally(context, uiState.destination, prompt, texts)
                viewModel.onShared(uiState.destination)
            }
        }
    }
    // 추천 질문은 누르면 바로 묻는다. 질문이 상태에 반영돼 보낼 글이 다시 만들어진 다음에 보낸다.
    var pendingSuggestion by rememberSaveable { mutableStateOf<String?>(null) }
    val currentSend by rememberUpdatedState(send)
    LaunchedEffect(pendingSuggestion, uiState.question) {
        if (pendingSuggestion != null && uiState.question == pendingSuggestion) {
            pendingSuggestion = null
            currentSend()
        }
    }
    AiHubScreen(
        uiState = uiState,
        promptText = prompt,
        actions = AiHubActions(
            onBack = onBack,
            onQuestionChanged = viewModel::onQuestionChanged,
            onIncludeRecordsChanged = viewModel::onIncludeRecordsChanged,
            onIncludeCostsChanged = viewModel::onIncludeCostsChanged,
            onSelectDestination = viewModel::selectDestination,
            onSend = send,
            onAskSuggestion = { text ->
                viewModel.onQuestionChanged(text)
                pendingSuggestion = text
            },
            onStop = viewModel::stopAnswering,
            onNewChat = viewModel::clearConversation,
            onDownloadModel = viewModel::downloadModel,
        ),
    )
}

data class AiHubActions(
    val onBack: () -> Unit = {},
    val onQuestionChanged: (String) -> Unit = {},
    val onIncludeRecordsChanged: (Boolean) -> Unit = {},
    val onIncludeCostsChanged: (Boolean) -> Unit = {},
    val onSelectDestination: (AiDestination) -> Unit = {},
    val onSend: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onNewChat: () -> Unit = {},
    val onOpenContext: () -> Unit = {},
    val onDownloadModel: () -> Unit = {},
    /** 추천 질문을 눌렀을 때. 입력창에 넣고 바로 보낸다. */
    val onAskSuggestion: (String) -> Unit = {},
)

/**
 * Gemini처럼 위에는 답할 AI 고르기, 가운데는 인사와 제안 카드(대화가 시작되면 대화), 아래에는 둥근 입력창.
 * 넓은 화면에서도 글 폭이 너무 길어지지 않게 가운데에 모은다. 보낼 정보는 칩을 눌러 시트로 본다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(uiState: AiHubUiState, promptText: String?, actions: AiHubActions, modifier: Modifier = Modifier) {
    var isContextOpen by rememberSaveable { mutableStateOf(false) }
    val screenActions = actions.copy(onOpenContext = { isContextOpen = true })
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { DestinationPicker(uiState, actions.onSelectDestination) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ai_back))
                    }
                },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = actions.onNewChat) {
                            Icon(Icons.Filled.AddComment, contentDescription = stringResource(R.string.ai_new_chat))
                        }
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // 탭 바 없이 화면을 덮으므로 시스템 내비게이션 영역과 키보드를 직접 피한다.
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val contentModifier = Modifier
                .widthIn(max = CONTENT_MAX_WIDTH)
                .fillMaxWidth()
                .padding(horizontal = ChageunTheme.spacing.gutter)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                if (uiState.messages.isEmpty()) {
                    Column(
                        contentModifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = ChageunTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
                    ) {
                        AiWelcome(
                            vehicleName = uiState.facts?.let { "${it.maker} ${it.model}".trim() },
                            // 모델이 준비되기 전에는 눌러도 보낼 수 없으니 추천 대신 준비 카드를 보여 준다.
                            suggestions = if (uiState.needsOnDeviceSetup) {
                                emptyList()
                            } else {
                                suggestions(uiState.options.focusItem, uiState.facts?.focusRecord)
                            },
                            onSuggestion = actions.onAskSuggestion,
                        )
                        if (uiState.needsOnDeviceSetup) {
                            OnDeviceSetupCard(
                                state = uiState.onDevice,
                                onDownload = actions.onDownloadModel,
                                onUseWeb = { actions.onSelectDestination(AiDestination.ChatGpt) },
                            )
                        }
                    }
                } else {
                    Conversation(uiState.messages, contentModifier)
                }
            }
            AiComposer(
                uiState = uiState,
                actions = screenActions,
                modifier = contentModifier.padding(vertical = ChageunTheme.spacing.sm),
            )
            Text(
                stringResource(R.string.ai_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = contentModifier.padding(bottom = ChageunTheme.spacing.sm),
            )
        }
    }
    if (isContextOpen) {
        AdaptiveSheet(isExpanded = isExpandedWidth(), onDismiss = { isContextOpen = false }) {
            ContextPreview(uiState, promptText, actions)
        }
    }
}

@Composable
private fun Conversation(messages: List<ChatMessage>, modifier: Modifier) {
    val listState = rememberLazyListState()
    // 새 메시지가 붙거나 답이 길어지면 맨 아래를 따라간다.
    val last = messages.lastOrNull()
    LaunchedEffect(messages.size, last?.text?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.lg),
        contentPadding = PaddingValues(vertical = ChageunTheme.spacing.md),
    ) {
        itemsIndexed(messages) { _, message -> ChatBubble(message) }
    }
}

private val CONTENT_MAX_WIDTH = 720.dp
