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
class FakeAuthRepository : AuthRepository {
    private val user = MutableStateFlow<AuthUser?>(null)
    private val passwords = mutableMapOf<String, String>()
    private val verified = mutableSetOf<String>()

    var nextError: AuthError? = null
    val passwordResets = mutableListOf<String>()
    var verificationMails = 0
        private set

    override val currentUser: StateFlow<AuthUser?> = user

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

    /** "new"로 시작하는 토큰은 처음 쓰는 Google 계정이다. */
    override suspend fun signInWithGoogle(idToken: String): AuthResult = respond {
        user.value = AuthUser("google-uid", "driver@gmail.com", "Driver", true, setOf(AuthMethod.Google))
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
