package com.naury.chageun.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.ImportPreview
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

enum class DataMessage { ExportDone, ExportFailed, ImportDone, ImportFailed, ImportUnsupported, ImportInvalid }

data class PendingImport(val sourceUri: String, val preview: ImportPreview.Ready)

data class DataUiState(
    /** 삭제 확인이 표시되는 동안에만 null이 아니다. */
    val pendingDeletion: LocalDataSummary? = null,
    val pendingImport: PendingImport? = null,
    val message: DataMessage? = null,
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

    fun setMileageReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMileageReminderEnabled(enabled) }
    }

    fun export(destinationUri: String) {
        _dataState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            val succeeded = backupRepository.export(destinationUri)
            _dataState.update {
                it.copy(
                    isWorking = false,
                    message = if (succeeded) DataMessage.ExportDone else DataMessage.ExportFailed,
                )
            }
        }
    }

    fun previewImport(sourceUri: String) {
        _dataState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            val preview = backupRepository.previewImport(sourceUri)
            _dataState.update {
                when (preview) {
                    is ImportPreview.Ready -> it.copy(
                        isWorking = false,
                        pendingImport = PendingImport(sourceUri, preview),
                    )
                    is ImportPreview.UnsupportedVersion -> it.copy(
                        isWorking = false,
                        message = DataMessage.ImportUnsupported,
                    )
                    ImportPreview.Invalid -> it.copy(isWorking = false, message = DataMessage.ImportInvalid)
                }
            }
        }
    }

    fun cancelImport() = _dataState.update { it.copy(pendingImport = null) }

    fun confirmImport() {
        val pending = _dataState.value.pendingImport ?: return
        _dataState.update { it.copy(pendingImport = null, isWorking = true) }
        viewModelScope.launch {
            val succeeded = backupRepository.import(pending.sourceUri)
            _dataState.update {
                it.copy(
                    isWorking = false,
                    message = if (succeeded) DataMessage.ImportDone else DataMessage.ImportFailed,
                )
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

    /** 삭제 후에는 대표 차량이 없으므로 앱 루트가 알아서 온보딩으로 돌아간다. */
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
