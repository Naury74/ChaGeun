package com.naury.chageun.feature.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.auth.GoogleIdTokenSource
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.AuthResult
import com.naury.chageun.core.domain.auth.GoogleIdTokenResult
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeleteAccountUiState(
    val user: AuthUser? = null,
    val password: String = "",
    val isBusy: Boolean = false,
    val error: AuthError? = null,
    val isDeleted: Boolean = false,
) {
    /** 이메일로 가입한 계정은 비밀번호로, Google 계정은 계정 선택으로 다시 확인한다. */
    val confirmsWithPassword: Boolean get() = user?.methods?.contains(AuthMethod.Email) == true
    val canDelete: Boolean get() = !isBusy && user != null && (!confirmsWithPassword || password.isNotEmpty())
}

/**
 * AC07. 다시 확인한 뒤 계정을 지운다. 백업은 사용자 본인 드라이브에 있어 운영자가 지울 수 없으므로
 * 화면에서 드라이브에서 지우는 방법을 안내한다(ADR-006).
 */
@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleIdTokens: GoogleIdTokenSource,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val state = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = state.asStateFlow()

    init {
        viewModelScope.launch { state.update { it.copy(user = authRepository.currentUser.first()) } }
    }

    fun setPassword(password: String) = state.update { it.copy(password = password, error = null) }

    /** Google 계정은 계정 선택 창을 띄우므로 [activityContext]가 필요하다. */
    fun delete(activityContext: Context) {
        val current = state.value
        if (!current.canDelete) return
        state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            val error = when (val check = reauthenticate(current, activityContext)) {
                Reauthentication.Cancelled -> {
                    state.update { it.copy(isBusy = false) }
                    return@launch
                }
                is Reauthentication.Failed -> check.error
                Reauthentication.Confirmed -> (authRepository.deleteAccount() as? AuthResult.Failure)?.error
            }
            if (error == null) analytics.track(AnalyticsEvent.AccountDeleted)
            state.update { it.copy(isBusy = false, error = error, isDeleted = error == null) }
        }
    }

    private suspend fun reauthenticate(current: DeleteAccountUiState, activityContext: Context): Reauthentication {
        val result = if (current.confirmsWithPassword) {
            authRepository.reauthenticateWithPassword(current.password)
        } else {
            when (val token = googleIdTokens.request(activityContext)) {
                is GoogleIdTokenResult.Token -> authRepository.reauthenticateWithGoogle(token.idToken)
                // 사용자가 계정 선택을 닫았다. 오류로 보이지 않는다.
                GoogleIdTokenResult.Cancelled -> return Reauthentication.Cancelled
                GoogleIdTokenResult.NoAccount -> AuthResult.Failure(AuthError.NoGoogleAccount)
                GoogleIdTokenResult.Unavailable -> AuthResult.Failure(AuthError.Unavailable)
                GoogleIdTokenResult.Failed -> AuthResult.Failure(AuthError.Unknown)
            }
        }
        return when (result) {
            is AuthResult.Success -> Reauthentication.Confirmed
            is AuthResult.Failure -> Reauthentication.Failed(result.error)
        }
    }

    private sealed interface Reauthentication {
        data object Confirmed : Reauthentication

        data object Cancelled : Reauthentication

        data class Failed(val error: AuthError) : Reauthentication
    }
}
