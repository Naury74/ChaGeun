package com.naury.chageun.feature.account

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.auth.DriveConnectRequest
import com.naury.chageun.core.auth.GoogleDriveAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DriveConnectionUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    /** 화면이 띄워야 할 Google 권한 창. 띄운 뒤에는 [DriveConnectionViewModel.onConsentShown]을 부른다. */
    val consent: IntentSender? = null,
    val hasFailed: Boolean = false,
)

/** Google 드라이브 연결과 연결 해제. 권한 창은 Activity가 띄워야 해서 요청만 상태로 내보낸다. */
@HiltViewModel
class DriveConnectionViewModel @Inject constructor(private val driveAccess: GoogleDriveAccess) : ViewModel() {

    private val progress = MutableStateFlow(DriveConnectionUiState())

    val uiState: StateFlow<DriveConnectionUiState> = combine(driveAccess.isConnected, progress) { connected, step ->
        step.copy(isConnected = connected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DriveConnectionUiState())

    fun connect() {
        if (progress.value.isConnecting) return
        progress.value = DriveConnectionUiState(isConnecting = true)
        viewModelScope.launch {
            progress.value = when (val request = driveAccess.connect()) {
                DriveConnectRequest.Connected -> DriveConnectionUiState()
                is DriveConnectRequest.NeedsConsent -> DriveConnectionUiState(
                    isConnecting = true,
                    consent = request.intentSender,
                )
                DriveConnectRequest.Failed -> DriveConnectionUiState(hasFailed = true)
            }
        }
    }

    fun onConsentShown() {
        progress.value = progress.value.copy(consent = null)
    }

    /** 권한 창에서 돌아왔다. 사용자가 닫았으면 [data]가 null이고 조용히 끝낸다. */
    fun onConsentResult(data: Intent?) {
        viewModelScope.launch {
            val granted = data != null && driveAccess.completeConsent(data)
            progress.value = DriveConnectionUiState(hasFailed = data != null && !granted)
        }
    }

    fun disconnect() {
        driveAccess.disconnect()
        progress.value = DriveConnectionUiState()
    }

    fun dismissFailure() {
        progress.value = progress.value.copy(hasFailed = false)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
