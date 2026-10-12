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
import com.naury.chageun.core.domain.auth.ProfileNameRepository
import com.naury.chageun.core.domain.auth.ProfilePhotoRepository
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.model.ProfileNameCheck
import com.naury.chageun.core.model.ProfileNameRule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    /** 받아 둔 Google 프로필 사진 파일. 없으면 이름 첫 글자를 보인다. */
    val photoPath: String? = null,
    val isGoogleConfigured: Boolean = false,
    val isBusy: Boolean = false,
    val notice: AccountNotice? = null,
    /** 부를 이름을 고치는 창. 열려 있지 않으면 null이다. */
    val nameEditor: NameEditorState? = null,
)

/**
 * 부를 이름 고치기 창의 상태. [initial]은 창을 열 때 보이던 이름이다.
 * [hasCustomName]이면 직접 정한 이름이 있어 기본 이름(Google 이름이나 이메일 앞부분)으로 돌아갈 수 있다.
 */
data class NameEditorState(
    val draft: String,
    val initial: String,
    val hasCustomName: Boolean,
    val usesGoogleName: Boolean,
    val isSaving: Boolean = false,
    val error: AuthError? = null,
) {
    val check: ProfileNameCheck get() = ProfileNameRule.check(draft)

    /** 저장해도 보이는 이름이 그대로라 서버에 묻지 않아도 되는지. */
    internal fun isUnchanged(name: String?): Boolean = name == initial || (name == null && !hasCustomName)
}

/** 로그인 전에는 계정 시작(AC01), 로그인 뒤에는 계정 화면(AC05)과 인증 안내(AC04)를 맡는다. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleIdTokens: GoogleIdTokenSource,
    private val analytics: AnalyticsTracker,
    profilePhotos: ProfilePhotoRepository,
    private val profileNames: ProfileNameRepository,
) : ViewModel() {

    private val isBusy = MutableStateFlow(false)
    private val notice = MutableStateFlow<AccountNotice?>(null)
    private val nameEditor = MutableStateFlow<NameEditorState?>(null)

    val uiState: StateFlow<AccountUiState> = combine(
        authRepository.currentUser,
        profilePhotos.photoPath,
        isBusy,
        notice,
        nameEditor,
    ) { user, photo, busy, message, editor ->
        AccountUiState(
            isLoading = false,
            user = user,
            photoPath = photo,
            isGoogleConfigured = googleIdTokens.isConfigured,
            isBusy = busy,
            notice = message,
            // 로그아웃하면 열려 있던 창도 닫는다.
            nameEditor = editor?.takeIf { user != null },
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
                // 인증을 마친 이때가 가입 완료다.
                analytics.track(AnalyticsEvent.SignUp(AuthMethod.Email))
                AccountNotice.Verified
            } else {
                AccountNotice.NotVerifiedYet
            }
        }
    }

    fun signOut() = run {
        nameEditor.value = null
        authRepository.signOut()
    }

    /** 인증 전 계정을 지우고 처음부터 다시 가입한다. 지우지 못하면 로그아웃만 한다. */
    fun startOver() = run {
        if (authRepository.deleteAccount() is AuthResult.Failure) authRepository.signOut()
    }

    fun dismissNotice() {
        notice.value = null
    }

    /** 지금 보이는 이름을 채운 채로 부를 이름 고치기 창을 연다. */
    fun editName() {
        val user = uiState.value.user ?: return
        val name = user.profileName.orEmpty()
        nameEditor.value = NameEditorState(
            draft = name,
            initial = name,
            // Google로 가입하면 계정 이름이 그때의 Google 이름으로 채워진다. 같으면 따로 정한 이름으로 보지 않는다.
            hasCustomName = !user.displayName.isNullOrBlank() && user.displayName?.trim() != user.googleName?.trim(),
            usesGoogleName = !user.googleName.isNullOrBlank(),
        )
    }

    fun changeNameDraft(text: String) {
        nameEditor.update { editor ->
            if (editor == null || editor.isSaving) editor else editor.copy(draft = text, error = null)
        }
    }

    /** 저장하는 동안에는 닫지 않는다. 결과를 기다리는 중에 창이 사라지면 실패를 알릴 곳이 없다. */
    fun closeNameEditor() {
        nameEditor.update { it?.takeIf { editor -> editor.isSaving } }
    }

    /**
     * 입력한 이름을 계정에 저장한다. 비어 있거나 [useDefaultName]이면 정한 이름을 지워 기본 이름으로 돌아간다.
     * 실패하면 창과 입력한 글자를 그대로 두고 이유만 보여 준다.
     */
    fun saveName(useDefaultName: Boolean = false) {
        val editor = nameEditor.value?.takeUnless { it.isSaving } ?: return
        val check = if (useDefaultName) ProfileNameCheck.Empty else editor.check
        val name = when (check) {
            is ProfileNameCheck.Valid -> check.name
            ProfileNameCheck.Empty -> null
            ProfileNameCheck.TooLong, ProfileNameCheck.HasLineBreak -> return
        }
        if (editor.isUnchanged(name)) {
            nameEditor.value = null
            return
        }
        nameEditor.value = editor.copy(isSaving = true, error = null)
        viewModelScope.launch {
            nameEditor.value = when (val result = profileNames.setDisplayName(name)) {
                is AuthResult.Success -> null
                is AuthResult.Failure -> editor.copy(error = result.error)
            }
        }
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
