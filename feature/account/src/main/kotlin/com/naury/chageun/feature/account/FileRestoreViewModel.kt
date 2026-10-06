package com.naury.chageun.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.ImportPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileRestoreUiState(
    /** 고른 파일과 미리보기. 확인 창이 떠 있는 동안만 있다. */
    val pendingUri: String? = null,
    val preview: ImportPreview.Ready? = null,
    val isWorking: Boolean = false,
    val notice: BackupNotice? = null,
)

/** 새 기기 온보딩에서 다른 기기가 보낸 ZIP 파일로 시작한다. 복원으로 차량이 생기면 앱이 홈으로 넘어간다. */
@HiltViewModel
class FileRestoreViewModel @Inject constructor(private val backupRepository: BackupRepository) : ViewModel() {

    private val state = MutableStateFlow(FileRestoreUiState())
    val uiState: StateFlow<FileRestoreUiState> = state.asStateFlow()

    fun preview(uri: String) {
        state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val preview = backupRepository.previewImport(uri)
            state.update {
                when (preview) {
                    is ImportPreview.Ready -> FileRestoreUiState(pendingUri = uri, preview = preview)
                    is ImportPreview.UnsupportedVersion -> FileRestoreUiState(notice = BackupNotice.NeedsUpdate)
                    ImportPreview.Invalid -> FileRestoreUiState(notice = BackupNotice.RestoreFailed)
                }
            }
        }
    }

    fun confirm() {
        val uri = state.value.pendingUri ?: return
        state.update { FileRestoreUiState(isWorking = true) }
        viewModelScope.launch {
            val restored = backupRepository.import(uri)
            state.update {
                FileRestoreUiState(notice = if (restored) BackupNotice.Restored else BackupNotice.RestoreFailed)
            }
        }
    }

    fun cancel() = state.update { FileRestoreUiState() }

    fun dismissNotice() = state.update { it.copy(notice = null) }
}
