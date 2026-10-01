package com.naury.chageun.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExportResult { Success, Failure }

data class DataUiState(
    /** Non-null while the delete confirmation is shown. */
    val pendingDeletion: LocalDataSummary? = null,
    val exportResult: ExportResult? = null,
    val isWorking: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserSettings())

    private val _dataState = MutableStateFlow(DataUiState())
    val dataState: StateFlow<DataUiState> = _dataState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setMaintenanceReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMaintenanceReminderEnabled(enabled) }
    }

    fun export(destinationUri: String) {
        _dataState.update { it.copy(isWorking = true, exportResult = null) }
        viewModelScope.launch {
            val succeeded = backupRepository.export(destinationUri)
            _dataState.update {
                it.copy(isWorking = false, exportResult = if (succeeded) ExportResult.Success else ExportResult.Failure)
            }
        }
    }

    fun requestDeleteAll() {
        viewModelScope.launch {
            val summary = backupRepository.summary()
            _dataState.update { it.copy(pendingDeletion = summary) }
        }
    }

    fun cancelDeleteAll() = _dataState.update { it.copy(pendingDeletion = null) }

    /** After deletion there is no primary vehicle, so the app root returns to onboarding by itself. */
    fun confirmDeleteAll() {
        _dataState.update { it.copy(pendingDeletion = null, isWorking = true) }
        viewModelScope.launch {
            backupRepository.deleteAll()
            _dataState.update { it.copy(isWorking = false) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
