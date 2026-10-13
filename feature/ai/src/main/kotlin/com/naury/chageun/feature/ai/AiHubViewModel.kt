package com.naury.chageun.feature.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.ai.AiContextFacts
import com.naury.chageun.core.domain.ai.AiContextOptions
import com.naury.chageun.core.domain.ai.BuildAiContextUseCase
import com.naury.chageun.core.domain.ai.OnDeviceAiException
import com.naury.chageun.core.domain.ai.OnDeviceAiStatus
import com.naury.chageun.core.domain.ai.OnDeviceAssistant
import com.naury.chageun.core.domain.ai.OnDeviceDownload
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.feature.ai.ondevice.dropUnfinishedSentence
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChatRole { User, Assistant }

data class ChatMessage(
    val role: ChatRole,
    val text: String,
    val isStreaming: Boolean = false,
    val isFailed: Boolean = false,
)

/** 기기 안 AI를 쓰기 전 모델 준비 상태. */
data class OnDeviceState(
    /** null이면 아직 확인하는 중이다. */
    val status: OnDeviceAiStatus? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val hasDownloadFailed: Boolean = false,
)

data class AiHubUiState(
    val question: String = "",
    val options: AiContextOptions = AiContextOptions(),
    val facts: AiContextFacts? = null,
    val isQuestionMissing: Boolean = false,
    val destination: AiDestination = AiDestination.ChatGpt,
    val onDevice: OnDeviceState = OnDeviceState(),
    /** 기기 안 AI와 나눈 대화. 화면을 닫으면 사라지고 어디에도 저장하지 않는다. */
    val messages: List<ChatMessage> = emptyList(),
) {
    val isAnswering: Boolean get() = messages.lastOrNull()?.isStreaming == true

    /** 차근 AI를 골랐지만 모델이 아직 준비되지 않아 안내 카드를 보여야 한다. */
    val needsOnDeviceSetup: Boolean
        get() = destination == AiDestination.OnDevice && onDevice.status != OnDeviceAiStatus.Available
}

/**
 * 질문은 이 화면의 saved state에만 둔다. 영구 저장하거나 로그에 남기지 않는다.
 * 기기 안 AI를 쓸 수 있으면 처음부터 그쪽을 고르고, 아니면 ChatGPT로 시작한다.
 */
@HiltViewModel(assistedFactory = AiHubViewModel.Factory::class)
class AiHubViewModel @AssistedInject constructor(
    @Assisted focusItem: MaintenanceItem?,
    @Assisted focusRecord: RecordRef?,
    private val savedStateHandle: SavedStateHandle,
    buildContext: BuildAiContextUseCase,
    private val onDeviceAssistant: OnDeviceAssistant,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val question = savedStateHandle.getStateFlow(KEY_QUESTION, "")
    private val includeRecords = savedStateHandle.getStateFlow(KEY_RECORDS, true)
    private val includeCosts = savedStateHandle.getStateFlow(KEY_COSTS, false)
    private val isQuestionMissing = savedStateHandle.getStateFlow(KEY_MISSING, false)
    private val destination = savedStateHandle.getStateFlow<AiDestination?>(KEY_DESTINATION, null)
    private val onDevice = MutableStateFlow(OnDeviceState())
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var answerJob: Job? = null

    private val options = combine(includeRecords, includeCosts) { records, costs ->
        AiContextOptions(
            includeRecords = records,
            includeCosts = costs,
            focusItem = focusItem,
            focusRecord = focusRecord,
        )
    }

    private val input =
        combine(question, isQuestionMissing, destination) { q, missing, chosen -> Triple(q, missing, chosen) }

    val uiState: StateFlow<AiHubUiState> =
        combine(input, options, buildContext(options), onDevice, messages) {
                (q, missing, chosen),
                opts,
                facts,
                device,
                chat,
            ->
            AiHubUiState(
                question = q,
                options = opts,
                facts = facts,
                isQuestionMissing = missing,
                destination = chosen ?: defaultDestination(device.status),
                onDevice = device,
                messages = chat,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            AiHubUiState(options = AiContextOptions(focusItem = focusItem, focusRecord = focusRecord)),
        )

    init {
        viewModelScope.launch {
            val status = onDeviceAssistant.status()
            onDevice.update { it.copy(status = status) }
            // 시스템이 이미 내려받는 중이면 진행률을 이어서 보여 준다.
            if (status == OnDeviceAiStatus.Downloading) downloadModel()
        }
    }

    /** 안내 카드에서 사용자가 누를 때만 모델을 내려받는다. 용량이 커서 몰래 받지 않는다. */
    fun downloadModel() {
        val state = onDevice.value
        val canDownload = state.status == OnDeviceAiStatus.Downloadable || state.status == OnDeviceAiStatus.Downloading
        if (state.isDownloading || !canDownload) return
        // 두 번 눌러도 한 번만 받도록 코루틴을 띄우기 전에 표시한다.
        onDevice.update { it.copy(isDownloading = true, downloadProgress = null, hasDownloadFailed = false) }
        viewModelScope.launch { watchDownload() }
    }

    fun onQuestionChanged(value: String) {
        // 질문 원문은 기록하지 않고, 화면당 처음 입력을 시작한 사실만 남긴다.
        if (value.isNotBlank() && savedStateHandle.get<Boolean>(KEY_STARTED) != true) {
            savedStateHandle[KEY_STARTED] = true
            analytics.track(AnalyticsEvent.AiQuestionStarted)
        }
        savedStateHandle[KEY_QUESTION] = value.take(MAX_QUESTION_LENGTH)
        savedStateHandle[KEY_MISSING] = false
    }

    fun onIncludeRecordsChanged(value: Boolean) {
        savedStateHandle[KEY_RECORDS] = value
    }

    fun onIncludeCostsChanged(value: Boolean) {
        savedStateHandle[KEY_COSTS] = value
    }

    fun selectDestination(value: AiDestination) {
        savedStateHandle[KEY_DESTINATION] = value
    }

    /** @return 질문이 있고 컨텍스트를 보내도 될 때 true */
    fun validateBeforeShare(): Boolean {
        val isValid = question.value.isNotBlank()
        savedStateHandle[KEY_MISSING] = !isValid
        return isValid
    }

    fun onShared(destination: AiDestination) {
        analytics.track(AnalyticsEvent.AiShareCompleted(destination.analyticsTarget))
    }

    /**
     * 기기 안 AI에게 묻는다. [contextPrompt]는 차 정보와 질문이 담긴 글, [instruction]은 답변 방식이다.
     * 앞선 대화가 있으면 짧게 붙여 이어지는 질문을 이해하게 한다. 모델이 없으면 먼저 내려받는다.
     */
    fun ask(contextPrompt: String, instruction: String, historyLabel: String) {
        if (uiState.value.isAnswering || onDevice.value.status != OnDeviceAiStatus.Available) return
        if (!validateBeforeShare()) return
        val asked = question.value.trim()
        val prompt = historyPrefix(historyLabel) + contextPrompt
        savedStateHandle[KEY_QUESTION] = ""
        messages.update {
            it + ChatMessage(ChatRole.User, asked) +
                ChatMessage(ChatRole.Assistant, "", isStreaming = true)
        }
        onShared(AiDestination.OnDevice)
        answerJob = viewModelScope.launch {
            try {
                onDeviceAssistant.answer(instruction, prompt).collect { text -> replaceLast { it.copy(text = text) } }
                replaceLast { it.copy(text = dropUnfinishedSentence(it.text), isStreaming = false) }
            } catch (_: OnDeviceAiException) {
                replaceLast { it.copy(isStreaming = false, isFailed = it.text.isBlank()) }
            }
        }
    }

    /** 답을 만드는 중에 멈춘다. 지금까지 나온 답은 남긴다. */
    fun stopAnswering() {
        answerJob?.cancel()
        replaceLast { if (it.isStreaming) it.copy(isStreaming = false, isFailed = it.text.isBlank()) else it }
    }

    fun clearConversation() {
        stopAnswering()
        messages.value = emptyList()
    }

    private suspend fun watchDownload() {
        var completed = false
        onDeviceAssistant.download().collect { download ->
            when (download) {
                is OnDeviceDownload.InProgress -> onDevice.update { it.copy(downloadProgress = download.progress) }
                OnDeviceDownload.Completed -> completed = true
                OnDeviceDownload.Failed -> completed = false
            }
        }
        onDevice.update {
            it.copy(
                status = if (completed) OnDeviceAiStatus.Available else it.status,
                isDownloading = false,
                hasDownloadFailed = !completed,
            )
        }
    }

    /** 이어지는 질문을 위해 최근 대화 두 번을 짧게 붙인다. 기기 안 모델은 입력 길이가 짧다. */
    private fun historyPrefix(label: String): String {
        val turns = messages.value.filter { !it.isFailed && it.text.isNotBlank() }.takeLast(HISTORY_MESSAGES)
        if (turns.isEmpty()) return ""
        return buildString {
            appendLine(label)
            turns.forEach {
                appendLine("${if (it.role == ChatRole.User) "Q" else "A"}: ${it.text.take(HISTORY_CHARS)}")
            }
            appendLine()
        }
    }

    private fun replaceLast(transform: (ChatMessage) -> ChatMessage) {
        messages.update { list -> if (list.isEmpty()) list else list.dropLast(1) + transform(list.last()) }
    }

    private fun defaultDestination(status: OnDeviceAiStatus?): AiDestination =
        if (status == null || status == OnDeviceAiStatus.Unavailable) AiDestination.ChatGpt else AiDestination.OnDevice

    @AssistedFactory
    interface Factory {
        fun create(focusItem: MaintenanceItem?, focusRecord: RecordRef?): AiHubViewModel
    }

    private companion object {
        const val KEY_QUESTION = "ai_question"
        const val KEY_STARTED = "ai_question_started"
        const val KEY_RECORDS = "ai_include_records"
        const val KEY_COSTS = "ai_include_costs"
        const val KEY_MISSING = "ai_question_missing"
        const val KEY_DESTINATION = "ai_destination"
        const val MAX_QUESTION_LENGTH = 500
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val HISTORY_MESSAGES = 4
        const val HISTORY_CHARS = 400
    }
}
