package com.naury.chageun.core.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.AuthResult
import com.naury.chageun.core.domain.auth.ProfileNameRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

/** AuthStateListener가 알려 주지 않는 계정 변화(인증 확인, 이름 변경)가 있을 때 currentUser를 다시 읽게 한다. */
internal class AuthUserRefresher {
    private val ticks = MutableStateFlow(0)
    val changes: StateFlow<Int> = ticks

    fun refresh() = ticks.update { it + 1 }
}

/** 부를 이름을 Firebase 계정의 표시 이름에 저장한다. 이름은 로그에 남기지 않는다. */
internal class FirebaseProfileNameRepository(
    private val auth: FirebaseAuth,
    private val refresher: AuthUserRefresher,
    private val logger: AppLogger,
) : ProfileNameRepository {

    override suspend fun setDisplayName(name: String?): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Failure(AuthError.Unknown)
        // null을 넣으면 Firebase가 표시 이름을 지운다.
        val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        return runCatching { user.updateProfile(request).await() }.fold(
            onSuccess = {
                refresher.refresh()
                AuthResult.Success()
            },
            onFailure = { e ->
                if (e is CancellationException) throw e
                val error = e.toAuthError()
                if (error == AuthError.Unknown) logger.warn("auth_update_name_failed", error = e)
                AuthResult.Failure(error)
            },
        )
    }
}

/** Firebase 없이 빌드했을 때. 로그인할 수 없으니 이름도 바꿀 수 없다. */
internal object UnavailableProfileNameRepository : ProfileNameRepository {
    override suspend fun setDisplayName(name: String?) = AuthResult.Failure(AuthError.Unavailable)
}
