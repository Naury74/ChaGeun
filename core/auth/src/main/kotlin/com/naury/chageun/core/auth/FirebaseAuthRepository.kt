package com.naury.chageun.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.AuthResult
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

internal class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val context: Context,
    private val logger: AppLogger,
    private val profilePhotos: ProfilePhotoCache,
    private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    // reload()로 인증 여부가 바뀌어도 AuthStateListener는 다시 불리지 않는다. 직접 다시 읽게 한다.
    private val reloads = MutableStateFlow(0)

    init {
        // 인증·재설정 메일을 앱 언어로 보낸다.
        auth.useAppLanguage()
    }

    override val currentUser: Flow<AuthUser?> = combine(authStateChanges(), reloads) { _, _ ->
        auth.currentUser?.toModel()
    }.distinctUntilChanged()

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult = attempt("sign_up") {
        val user = checkNotNull(auth.createUserWithEmailAndPassword(email.trim(), password).await().user)
        // 메일을 못 보내도 가입은 끝났다. 계정 화면에서 다시 보낼 수 있다.
        runCatching { user.sendEmailVerification().await() }
            .onFailure { logger.warn("auth_verification_mail_failed", error = it) }
        AuthResult.Success(isNewUser = true)
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult = attempt("sign_in") {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        AuthResult.Success()
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult = attempt("google_sign_in") {
        val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        AuthResult.Success(isNewUser = result.additionalUserInfo?.isNewUser == true)
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = attempt("password_reset") {
        auth.sendPasswordResetEmail(email.trim()).await()
        AuthResult.Success()
    }

    override suspend fun sendEmailVerification(): AuthResult = attempt("verification_mail") {
        checkNotNull(auth.currentUser).sendEmailVerification().await()
        AuthResult.Success()
    }

    override suspend fun reload(): AuthResult = attempt("reload") {
        auth.currentUser?.reload()?.await()
        reloads.value++
        AuthResult.Success()
    }

    override suspend fun signOut() {
        auth.signOut()
        clearRememberedCredential()
        clearProfilePhoto()
    }

    override suspend fun reauthenticateWithPassword(password: String): AuthResult = attempt("reauth_password") {
        val user = checkNotNull(auth.currentUser)
        user.reauthenticate(EmailAuthProvider.getCredential(checkNotNull(user.email), password)).await()
        AuthResult.Success()
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): AuthResult = attempt("reauth_google") {
        checkNotNull(auth.currentUser).reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
        AuthResult.Success()
    }

    override suspend fun deleteAccount(): AuthResult = attempt("delete_account") {
        checkNotNull(auth.currentUser).delete().await()
        clearRememberedCredential()
        clearProfilePhoto()
        AuthResult.Success()
    }

    /** 다음 Google 로그인에서 계정을 다시 고를 수 있게 기억한 선택을 지운다. */
    private suspend fun clearRememberedCredential() {
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
            .onFailure { if (it is CancellationException) throw it }
    }

    /** 계정을 떠나면 받아 둔 프로필 사진도 기기에 남기지 않는다. */
    private suspend fun clearProfilePhoto() = withContext(ioDispatcher) { profilePhotos.clear() }

    private fun authStateChanges(): Flow<Unit> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(Unit) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    @Suppress("TooGenericExceptionCaught") // Firebase Task는 여러 예외를 던지고 모두 AuthError로 바꾼다.
    private suspend fun attempt(operation: String, block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val error = e.toAuthError()
        if (error == AuthError.Unknown) logger.warn("auth_${operation}_failed", error = e)
        AuthResult.Failure(error)
    }
}

/**
 * Firebase 사용자 정보의 이름·사진은 처음 가입할 때 한 번 채워진다. Google 제공자 정보는 Google로 로그인할 때마다
 * 새로 받아 오므로 Google 이름과 사진은 그쪽을 먼저 쓴다. 이메일로 가입한 뒤 Google을 연결한 계정도 이렇게 해야 보인다.
 */
private fun FirebaseUser.toModel(): AuthUser {
    val google = providerData.firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    return AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        isEmailVerified = isEmailVerified,
        methods = providerData.mapNotNullTo(mutableSetOf()) {
            when (it.providerId) {
                GoogleAuthProvider.PROVIDER_ID -> AuthMethod.Google
                "password" -> AuthMethod.Email
                else -> null
            }
        },
        googleName = google?.displayName,
        photoUrl = (google?.photoUrl ?: photoUrl)?.toString(),
    )
}
