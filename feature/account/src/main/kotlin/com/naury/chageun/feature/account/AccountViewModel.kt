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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 계정 화면 아래쪽에 잠깐 띄우는 안내. */
sealed interface AccountNotice {
    data class Failed(val error: AuthError) : AccountNotice

    data object VerificationSent : AccountNotice

    data object NotVerifiedYet : AccountNotice

    data object Verified : AccountNotice
}

data class AccountUiState(
    val isLoading: Boolean = true,
    val user: AuthUser? = null,
    val isGoogleConfigured: Boolean = false,
    val isBusy: Boolean = false,
    val notice: AccountNotice? = null,
)

/** 로그인 전에는 계정 시작(AC01), 로그인 뒤에는 계정 화면(AC05)과 인증 안내(AC04)를 맡는다. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleIdTokens: GoogleIdTokenSource,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val isBusy = MutableStateFlow(false)
    private val notice = MutableStateFlow<AccountNotice?>(null)

    val uiState: StateFlow<AccountUiState> =
        combine(authRepository.currentUser, isBusy, notice) { user, busy, message ->
            AccountUiState(
                isLoading = false,
                user = user,
                isGoogleConfigured = googleIdTokens.isConfigured,
                isBusy = busy,
                notice = message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AccountUiState())

    /** 계정 선택 창은 Activity 위에 떠야 하므로 [activityContext]를 받는다. 창이 닫히면 바로 놓는다. */
    fun signInWithGoogle(activityContext: Context) = run {
        when (val token = googleIdTokens.request(activityContext)) {
            is GoogleIdTokenResult.Token -> onGoogleToken(token.idToken)
            GoogleIdTokenResult.Cancelled -> Unit
            GoogleIdTokenResult.NoAccount -> notice.value = AccountNotice.Failed(AuthError.NoGoogleAccount)
            GoogleIdTokenResult.Unavailable -> notice.value = AccountNotice.Failed(AuthError.Unavailable)
            GoogleIdTokenResult.Failed -> notice.value = AccountNotice.Failed(AuthError.Unknown)
        }
    }

    fun resendVerification() = run {
        notice.value = when (val result = authRepository.sendEmailVerification()) {
            is AuthResult.Success -> AccountNotice.VerificationSent
            is AuthResult.Failure -> AccountNotice.Failed(result.error)
        }
    }

    /** 메일 앱에서 인증 링크를 누르고 돌아왔을 때 누른다. */
    fun checkVerification() = run {
        notice.value = when (val result = authRepository.reload()) {
            is AuthResult.Failure -> AccountNotice.Failed(result.error)
            is AuthResult.Success -> if (authRepository.currentUser.first()?.needsEmailVerification == false) {
                AccountNotice.Verified
            } else {
                AccountNotice.NotVerifiedYet
            }
        }
    }

    fun signOut() = run { authRepository.signOut() }

    fun dismissNotice() {
        notice.value = null
    }

    private suspend fun onGoogleToken(idToken: String) {
        when (val result = authRepository.signInWithGoogle(idToken)) {
            is AuthResult.Success -> analytics.track(
                if (result.isNewUser) {
                    AnalyticsEvent.SignUp(
                        AuthMethod.Google,
                    )
                } else {
                    AnalyticsEvent.Login(AuthMethod.Google)
                },
            )
            is AuthResult.Failure -> notice.value = AccountNotice.Failed(result.error)
        }
    }

    /** 요청 하나가 끝날 때까지 버튼을 막는다. 이미 진행 중이면 무시한다. */
    private fun run(block: suspend () -> Unit) {
        if (isBusy.value) return
        isBusy.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                isBusy.value = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
