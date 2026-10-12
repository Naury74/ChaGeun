package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.AuthResult
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 이메일·비밀번호를 메모리에 두는 계정. [nextError]를 정하면 다음 요청 한 번이 그 오류로 실패한다.
 * [verifyEmailOutside]는 사용자가 메일 앱에서 인증 링크를 누른 것처럼 동작하고, [reload] 뒤에 반영된다.
 */
@Suppress("TooManyFunctions") // AuthRepository의 요청을 모두 흉내 낸다.
class FakeAuthRepository : AuthRepository {
    private val user = MutableStateFlow<AuthUser?>(null)
    private val passwords = mutableMapOf<String, String>()
    private val verified = mutableSetOf<String>()

    var nextError: AuthError? = null
    val passwordResets = mutableListOf<String>()
    var verificationMails = 0
        private set

    override val currentUser: StateFlow<AuthUser?> = user

    /** 테스트에서 원하는 계정으로 바로 로그인한 상태를 만든다. [FakeProfileNameRepository]도 이걸로 이름을 바꾼다. */
    var signedInUser: AuthUser?
        get() = user.value
        set(value) {
            user.value = value
        }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult = respond {
        if (email in passwords) return AuthResult.Failure(AuthError.EmailInUse)
        passwords[email] = password
        verificationMails++
        user.value = emailUser(email)
        AuthResult.Success(isNewUser = true)
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult = respond {
        if (passwords[email] != password) return AuthResult.Failure(AuthError.InvalidCredentials)
        user.value = emailUser(email)
        AuthResult.Success()
    }

    /** 다음 Google 로그인에서 Google 계정이 알려 줄 프로필 사진 주소. */
    var googlePhotoUrl: String? = null

    /** "new"로 시작하는 토큰은 처음 쓰는 Google 계정이다. */
    override suspend fun signInWithGoogle(idToken: String): AuthResult = respond {
        user.value = AuthUser(
            uid = "google-uid",
            email = "driver@gmail.com",
            displayName = "Driver",
            isEmailVerified = true,
            methods = setOf(AuthMethod.Google),
            googleName = "Driver",
            photoUrl = googlePhotoUrl,
        )
        AuthResult.Success(isNewUser = idToken.startsWith("new"))
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = respond {
        passwordResets += email
        AuthResult.Success()
    }

    override suspend fun sendEmailVerification(): AuthResult = respond {
        verificationMails++
        AuthResult.Success()
    }

    override suspend fun reload(): AuthResult = respond {
        user.value?.email?.let { user.value = emailUser(it) }
        AuthResult.Success()
    }

    override suspend fun signOut() {
        user.value = null
    }

    override suspend fun reauthenticateWithPassword(password: String): AuthResult = respond {
        val email = user.value?.email
        if (passwords[email] != password) return AuthResult.Failure(AuthError.InvalidCredentials)
        AuthResult.Success()
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): AuthResult = respond { AuthResult.Success() }

    override suspend fun deleteAccount(): AuthResult = respond {
        failDeletionWith?.let { return AuthResult.Failure(it) }
        user.value?.email?.let { passwords -= it }
        user.value = null
        deletedAccounts++
        AuthResult.Success()
    }

    var deletedAccounts = 0
        private set

    /** 다시 확인은 통과하고 삭제만 실패하게 할 때 쓴다. */
    var failDeletionWith: AuthError? = null

    fun verifyEmailOutside(email: String) {
        verified += email
    }

    private fun emailUser(email: String) =
        AuthUser("email-uid", email, null, email in verified, setOf(AuthMethod.Email))

    private inline fun respond(block: () -> AuthResult): AuthResult {
        val error = nextError ?: return block()
        nextError = null
        return AuthResult.Failure(error)
    }
}
