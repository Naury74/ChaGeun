package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.AuthResult
import com.naury.chageun.core.domain.auth.ProfileNameRepository

/** [auth]의 로그인한 계정 이름을 바로 바꾼다. [nextError]를 정하면 다음 저장 한 번이 그 오류로 실패한다. */
class FakeProfileNameRepository(private val auth: FakeAuthRepository) : ProfileNameRepository {
    var nextError: AuthError? = null

    /** 서버에 보낸 이름. null은 정한 이름을 지운 것이다. */
    val savedNames = mutableListOf<String?>()

    override suspend fun setDisplayName(name: String?): AuthResult {
        nextError?.let {
            nextError = null
            return AuthResult.Failure(it)
        }
        val user = auth.signedInUser ?: return AuthResult.Failure(AuthError.Unknown)
        savedNames += name
        auth.signedInUser = user.copy(displayName = name)
        return AuthResult.Success()
    }
}
