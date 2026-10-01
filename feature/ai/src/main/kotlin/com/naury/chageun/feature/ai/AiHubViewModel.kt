package com.naury.chageun.feature.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.ai.AiContextFacts
import com.naury.chageun.core.domain.ai.AiContextOptions
import com.naury.chageun.core.domain.ai.BuildAiContextUseCase
import com.naury.chageun.core.model.MaintenanceItem
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

/** The question lives only in saved state for this screen; it is never persisted or logged. */
@HiltViewModel(assistedFactory = AiHubViewModel.Factory::class)
class AiHubViewModel @AssistedInject constructor(
    @Assisted focusItem: MaintenanceItem?,
    private val savedStateHandle: SavedStateHandle,
    buildContext: BuildAiContextUseCase,
) : ViewModel() {

    private val question = savedStateHandle.getStateFlow(KEY_QUESTION, "")
    private val includeRecords = savedStateHandle.getStateFlow(KEY_RECORDS, true)
    private val includeCosts = savedStateHandle.getStateFlow(KEY_COSTS, false)
    private val isQuestionMissing = savedStateHandle.getStateFlow(KEY_MISSING, false)

    private val options = combine(includeRecords, includeCosts) { records, costs ->
        AiContextOptions(includeRecords = records, includeCosts = costs, focusItem = focusItem)
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
        AiHubUiState(options = AiContextOptions(focusItem = focusItem)),
    )

    fun onQuestionChanged(value: String) {
        savedStateHandle[KEY_QUESTION] = value.take(MAX_QUESTION_LENGTH)
        savedStateHandle[KEY_MISSING] = false
    }

    fun onIncludeRecordsChanged(value: Boolean) {
        savedStateHandle[KEY_RECORDS] = value
    }

    fun onIncludeCostsChanged(value: Boolean) {
        savedStateHandle[KEY_COSTS] = value
    }

    /** @return true when the question is present and the context may be shared. */
    fun validateBeforeShare(): Boolean {
        val isValid = question.value.isNotBlank()
        savedStateHandle[KEY_MISSING] = !isValid
        return isValid
    }

    @AssistedFactory
    interface Factory {
        fun create(focusItem: MaintenanceItem?): AiHubViewModel
    }

    private companion object {
        const val KEY_QUESTION = "ai_question"
        const val KEY_RECORDS = "ai_include_records"
        const val KEY_COSTS = "ai_include_costs"
        const val KEY_MISSING = "ai_question_missing"
        const val MAX_QUESTION_LENGTH = 500
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
