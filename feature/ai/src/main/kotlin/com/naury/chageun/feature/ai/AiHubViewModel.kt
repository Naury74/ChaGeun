package com.naury.chageun.feature.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.ai.AiContextFacts
import com.naury.chageun.core.domain.ai.AiContextOptions
import com.naury.chageun.core.domain.ai.BuildAiContextUseCase
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AiHubUiState(
    val question: String = "",
    val options: AiContextOptions = AiContextOptions(),
    val facts: AiContextFacts? = null,
    val isQuestionMissing: Boolean = false,
)

/** 질문은 이 화면의 saved state에만 둔다. 영구 저장하거나 로그에 남기지 않는다. */
@HiltViewModel(assistedFactory = AiHubViewModel.Factory::class)
class AiHubViewModel @AssistedInject constructor(
    @Assisted focusItem: MaintenanceItem?,
    @Assisted focusRecord: RecordRef?,
    private val savedStateHandle: SavedStateHandle,
    buildContext: BuildAiContextUseCase,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val question = savedStateHandle.getStateFlow(KEY_QUESTION, "")
    private val includeRecords = savedStateHandle.getStateFlow(KEY_RECORDS, true)
    private val includeCosts = savedStateHandle.getStateFlow(KEY_COSTS, false)
    private val isQuestionMissing = savedStateHandle.getStateFlow(KEY_MISSING, false)

    private val options = combine(includeRecords, includeCosts) { records, costs ->
        AiContextOptions(
            includeRecords = records,
            includeCosts = costs,
            focusItem = focusItem,
            focusRecord = focusRecord,
        )
    }

    val uiState: StateFlow<AiHubUiState> = combine(question, options, buildContext(options), isQuestionMissing) {
            q,
            opts,
            facts,
            missing,
        ->
        AiHubUiState(question = q, options = opts, facts = facts, isQuestionMissing = missing)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        AiHubUiState(options = AiContextOptions(focusItem = focusItem, focusRecord = focusRecord)),
    )

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

    /** @return 질문이 있고 컨텍스트를 공유해도 될 때 true */
    fun validateBeforeShare(): Boolean {
        val isValid = question.value.isNotBlank()
        savedStateHandle[KEY_MISSING] = !isValid
        return isValid
    }

    fun onShared(provider: AiProvider) {
        analytics.track(AnalyticsEvent.AiShareCompleted(provider.analyticsTarget))
    }

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
        const val MAX_QUESTION_LENGTH = 500
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
