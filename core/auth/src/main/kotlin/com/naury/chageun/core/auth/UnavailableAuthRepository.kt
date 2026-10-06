package com.naury.chageun.core.auth

import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.AuthResult
import com.naury.chageun.core.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** google-services.json 없이 빌드해 Firebase가 없을 때. 계정 화면은 열리지만 로그인은 준비 중으로 안내한다. */
internal object UnavailableAuthRepository : AuthRepository {
    private val unavailable = AuthResult.Failure(AuthError.Unavailable)

    override val currentUser: Flow<AuthUser?> = flowOf(null)

    override suspend fun signUpWithEmail(email: String, password: String) = unavailable

    override suspend fun signInWithEmail(email: String, password: String) = unavailable

    override suspend fun signInWithGoogle(idToken: String) = unavailable

    override suspend fun sendPasswordReset(email: String) = unavailable

    override suspend fun sendEmailVerification() = unavailable

    override suspend fun reload() = unavailable

    override suspend fun signOut() = Unit

    override suspend fun reauthenticateWithPassword(password: String) = unavailable

    override suspend fun reauthenticateWithGoogle(idToken: String) = unavailable

    override suspend fun deleteAccount() = unavailable
}
