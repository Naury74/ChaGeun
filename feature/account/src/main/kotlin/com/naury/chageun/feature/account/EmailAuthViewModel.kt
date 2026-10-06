package com.naury.chageun.feature.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.AuthResult
import com.naury.chageun.core.domain.auth.CredentialRules
import com.naury.chageun.core.model.AuthMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EmailAuthMode { SignIn, SignUp }

/** 비밀번호 찾기(AC03) 창. [isSent]면 메일을 보냈다는 안내로 바뀐다. */
data class PasswordResetState(
    val email: String,
    val isSent: Boolean = false,
    val isBusy: Boolean = false,
    val error: AuthError? = null,
) {
    val isEmailInvalid: Boolean get() = !CredentialRules.isValidEmail(email)
}

data class EmailAuthUiState(
    val mode: EmailAuthMode = EmailAuthMode.SignIn,
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val isPrivacyAgreed: Boolean = false,
    /** 처음엔 오류를 숨기고, 한 번 제출을 누른 뒤부터 보여 준다. */
    val showFieldErrors: Boolean = false,
    val isBusy: Boolean = false,
    val error: AuthError? = null,
    val reset: PasswordResetState? = null,
    val isCompleted: Boolean = false,
) {
    val isSignUp: Boolean get() = mode == EmailAuthMode.SignUp
    val isEmailInvalid: Boolean get() = !CredentialRules.isValidEmail(email)
    val isPasswordWeak: Boolean get() = isSignUp && !CredentialRules.isStrongPassword(password)
    val isConfirmMismatch: Boolean get() = isSignUp && passwordConfirm != password
    val canSubmit: Boolean
        get() = !isBusy &&
            email.isNotBlank() &&
            password.isNotEmpty() &&
            (!isSignUp || (passwordConfirm.isNotEmpty() && isPrivacyAgreed))
}

/**
 * 이메일 로그인·가입(AC02)과 비밀번호 찾기(AC03).
 *
 * 비밀번호는 프로세스가 죽은 뒤 복원되는 saved state에 남기지 않는다. 폴드 접기·펴기 같은 구성 변경에서는
 * ViewModel이 살아 있으므로 입력이 그대로 남는다.
 */
@HiltViewModel
class EmailAuthViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val state = MutableStateFlow(
        EmailAuthUiState(
            mode = savedStateHandle.get<String>(KEY_MODE)?.let(EmailAuthMode::valueOf) ?: EmailAuthMode.SignIn,
            email = savedStateHandle[KEY_EMAIL] ?: "",
        ),
    )
    val uiState: StateFlow<EmailAuthUiState> = state.asStateFlow()

    fun setMode(mode: EmailAuthMode) {
        savedStateHandle[KEY_MODE] = mode.name
        state.update { it.copy(mode = mode, passwordConfirm = "", showFieldErrors = false, error = null) }
    }

    fun setEmail(email: String) {
        savedStateHandle[KEY_EMAIL] = email
        state.update { it.copy(email = email, error = null) }
    }

    fun setPassword(password: String) = state.update { it.copy(password = password, error = null) }

    fun setPasswordConfirm(confirm: String) = state.update { it.copy(passwordConfirm = confirm) }

    fun setPrivacyAgreed(agreed: Boolean) = state.update { it.copy(isPrivacyAgreed = agreed) }

    fun submit() {
        val current = state.value
        if (!current.canSubmit) return
        if (current.isEmailInvalid || current.isPasswordWeak || current.isConfirmMismatch) {
            state.update { it.copy(showFieldErrors = true) }
            return
        }
        state.update { it.copy(isBusy = true, showFieldErrors = true, error = null) }
        viewModelScope.launch {
            val result = if (current.isSignUp) {
                authRepository.signUpWithEmail(current.email, current.password)
            } else {
                authRepository.signInWithEmail(current.email, current.password)
            }
            when (result) {
                is AuthResult.Success -> {
                    analytics.track(
                        if (current.isSignUp) {
                            AnalyticsEvent.SignUp(
                                AuthMethod.Email,
                            )
                        } else {
                            AnalyticsEvent.Login(AuthMethod.Email)
                        },
                    )
                    state.update { it.copy(isBusy = false, password = "", passwordConfirm = "", isCompleted = true) }
                }
                is AuthResult.Failure -> state.update { it.copy(isBusy = false, error = result.error) }
            }
        }
    }

    fun openPasswordReset() = state.update { it.copy(reset = PasswordResetState(email = it.email.trim())) }

    fun setResetEmail(email: String) = state.update { it.copy(reset = it.reset?.copy(email = email, error = null)) }

    fun sendPasswordReset() {
        val reset = state.value.reset ?: return
        if (reset.isBusy || reset.isEmailInvalid) return
        state.update { it.copy(reset = reset.copy(isBusy = true, error = null)) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(reset.email)
            state.update {
                it.copy(
                    reset = it.reset?.copy(
                        isBusy = false,
                        isSent = result is AuthResult.Success,
                        error = (result as? AuthResult.Failure)?.error,
                    ),
                )
            }
        }
    }

    fun closePasswordReset() = state.update { it.copy(reset = null) }

    private companion object {
        const val KEY_MODE = "account_email_mode"
        const val KEY_EMAIL = "account_email"
    }
}
