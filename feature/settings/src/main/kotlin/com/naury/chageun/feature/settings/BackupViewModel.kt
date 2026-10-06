package com.naury.chageun.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    /** 내보내기를 마친 공유용 파일. 화면이 공유 창을 띄운 뒤 비운다. */
    val readyToShare: String? = null,
)

/** 설정 > 데이터의 내보내기·가져오기·전체 삭제. 환경설정과 나눠 각 ViewModel의 책임을 좁힌다. */
@HiltViewModel
class BackupViewModel @Inject constructor(private val backupRepository: BackupRepository) : ViewModel() {

    private val _dataState = MutableStateFlow(DataUiState())
    val dataState: StateFlow<DataUiState> = _dataState.asStateFlow()

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

    /** [destinationUri]에 ZIP을 만들고, 성공하면 [shareUri]로 공유 창을 띄우게 한다. */
    fun exportForShare(destinationUri: String, shareUri: String) {
        _dataState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            val succeeded = backupRepository.export(destinationUri)
            _dataState.update {
                it.copy(
                    isWorking = false,
                    readyToShare = shareUri.takeIf { succeeded },
                    message = if (succeeded) null else DataMessage.ExportFailed,
                )
            }
        }
    }

    fun onShared() = _dataState.update { it.copy(readyToShare = null) }

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
}
