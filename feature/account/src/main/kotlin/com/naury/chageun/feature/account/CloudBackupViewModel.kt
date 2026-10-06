package com.naury.chageun.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import com.naury.chageun.core.domain.cloudbackup.CloudResult
import com.naury.chageun.core.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 복원은 내려받기 → 미리보기 확인 → 교체 순서로 진행한다. */
sealed interface RestoreStep {
    val backup: CloudBackup

    data class Downloading(override val backup: CloudBackup) : RestoreStep

    data class Confirming(override val backup: CloudBackup, val fileUri: String, val preview: ImportPreview.Ready) :
        RestoreStep

    data class Restoring(override val backup: CloudBackup) : RestoreStep
}

sealed interface BackupNotice {
    data object BackedUp : BackupNotice

    data object Restored : BackupNotice

    data object Deleted : BackupNotice

    /** 앱보다 새 형식으로 만든 백업이다. */
    data object NeedsUpdate : BackupNotice

    /** 내려받은 파일을 읽지 못했거나 교체에 실패했다. 기존 데이터는 그대로다. */
    data object RestoreFailed : BackupNotice

    data class Failed(val error: CloudBackupError) : BackupNotice
}

data class CloudBackupUiState(
    val backups: List<CloudBackup> = emptyList(),
    val isLoaded: Boolean = false,
    val isBackingUp: Boolean = false,
    /** 목록에서 고른 백업. 복원·삭제를 고르는 창을 띄운다. */
    val selected: CloudBackup? = null,
    val pendingDelete: CloudBackup? = null,
    val restore: RestoreStep? = null,
    val notice: BackupNotice? = null,
    val isAutoBackupEnabled: Boolean = false,
) {
    val lastBackup: CloudBackup? get() = backups.firstOrNull()
    val isWorking: Boolean get() = isBackingUp || restore != null
}

/** 계정 화면의 클라우드 백업(AC05)과 백업 목록(AC06). 창 상태도 여기 두어 폴드를 접고 펴도 남는다. */
@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val cloudBackups: CloudBackupRepository,
    private val localBackup: BackupRepository,
    private val settingsRepository: SettingsRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val state = MutableStateFlow(CloudBackupUiState())
    val uiState: StateFlow<CloudBackupUiState> = state.asStateFlow()

    init {
        // 다른 계정으로 바꾸거나 로그아웃하면 앞 계정의 목록을 지운다.
        viewModelScope.launch {
            authRepository.currentUser.map { it?.uid }.distinctUntilChanged().collect { uid ->
                state.update { CloudBackupUiState(isAutoBackupEnabled = it.isAutoBackupEnabled) }
                if (uid != null) refresh()
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.map { it.isCloudAutoBackupEnabled }.distinctUntilChanged().collect { enabled ->
                state.update { it.copy(isAutoBackupEnabled = enabled) }
            }
        }
    }

    /** 예약은 AutoBackupSync가 설정을 보고 맞춘다. */
    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCloudAutoBackupEnabled(enabled) }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = cloudBackups.list()) {
                is CloudResult.Success -> state.update { it.copy(backups = result.value, isLoaded = true) }
                is CloudResult.Failure -> state.update {
                    it.copy(isLoaded = true, notice = BackupNotice.Failed(result.error))
                }
            }
        }
    }

    fun backUpNow() {
        if (state.value.isWorking) return
        state.update { it.copy(isBackingUp = true) }
        viewModelScope.launch {
            val result = cloudBackups.backUpNow()
            if (result is CloudResult.Success) analytics.track(AnalyticsEvent.CloudBackupCreated)
            state.update {
                it.copy(
                    isBackingUp = false,
                    notice = when (result) {
                        is CloudResult.Success -> BackupNotice.BackedUp
                        is CloudResult.Failure -> BackupNotice.Failed(result.error)
                    },
                )
            }
            if (result is CloudResult.Success) refresh()
        }
    }

    fun select(backup: CloudBackup?) = state.update { it.copy(selected = backup) }

    fun startRestore(backup: CloudBackup) {
        if (state.value.isWorking) return
        state.update { it.copy(selected = null, restore = RestoreStep.Downloading(backup)) }
        viewModelScope.launch {
            val download = cloudBackups.download(backup.id)
            if (download is CloudResult.Failure) {
                state.update { it.copy(restore = null, notice = BackupNotice.Failed(download.error)) }
                return@launch
            }
            val fileUri = (download as CloudResult.Success).value
            when (val preview = localBackup.previewImport(fileUri)) {
                is ImportPreview.Ready ->
                    state.update { it.copy(restore = RestoreStep.Confirming(backup, fileUri, preview)) }
                is ImportPreview.UnsupportedVersion ->
                    state.update { it.copy(restore = null, notice = BackupNotice.NeedsUpdate) }
                ImportPreview.Invalid -> state.update { it.copy(restore = null, notice = BackupNotice.RestoreFailed) }
            }
        }
    }

    fun confirmRestore() {
        val step = state.value.restore as? RestoreStep.Confirming ?: return
        state.update { it.copy(restore = RestoreStep.Restoring(step.backup)) }
        viewModelScope.launch {
            val restored = localBackup.import(step.fileUri)
            if (restored) analytics.track(AnalyticsEvent.CloudBackupRestored)
            state.update {
                it.copy(restore = null, notice = if (restored) BackupNotice.Restored else BackupNotice.RestoreFailed)
            }
        }
    }

    /** 복원 확인·삭제 확인 창을 닫는다. 내려받기·교체 중인 복원은 끝날 때까지 둔다. */
    fun closeDialog() = state.update {
        it.copy(
            pendingDelete = null,
            restore = it.restore.takeUnless { step -> step is RestoreStep.Confirming },
        )
    }

    fun requestDelete(backup: CloudBackup) = state.update { it.copy(selected = null, pendingDelete = backup) }

    fun confirmDelete() {
        val backup = state.value.pendingDelete ?: return
        state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            when (val result = cloudBackups.delete(backup.id)) {
                is CloudResult.Success -> state.update {
                    it.copy(backups = it.backups - backup, notice = BackupNotice.Deleted)
                }
                is CloudResult.Failure -> state.update { it.copy(notice = BackupNotice.Failed(result.error)) }
            }
        }
    }

    fun dismissNotice() = state.update { it.copy(notice = null) }
}
