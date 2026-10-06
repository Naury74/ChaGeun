package com.naury.chageun.core.auth

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.naury.chageun.core.domain.auth.AuthError

/**
 * Firebase 예외를 화면에서 안내할 수 있는 종류로 바꾼다.
 *
 * 이메일 열거 보호가 켜진 프로젝트는 없는 계정과 틀린 비밀번호를 구분하지 않으므로 둘 다 [AuthError.InvalidCredentials]다.
 */
internal fun Throwable.toAuthError(): AuthError = when (this) {
    is FirebaseNetworkException -> AuthError.Network
    is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
    is FirebaseAuthRecentLoginRequiredException -> AuthError.RecentLoginRequired
    is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
    is FirebaseAuthUserCollisionException -> AuthError.EmailInUse
    is FirebaseAuthInvalidUserException ->
        if (errorCode == "ERROR_USER_DISABLED") AuthError.UserDisabled else AuthError.InvalidCredentials
    is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
    // 콘솔에서 로그인 방법을 켜지 않았을 때다.
    is FirebaseAuthException ->
        if (errorCode == "ERROR_OPERATION_NOT_ALLOWED") AuthError.Unavailable else AuthError.Unknown
    // 프로젝트에서 Authentication을 아직 시작하지 않았으면 코드 없이 메시지로만 온다.
    is FirebaseException ->
        if (message?.contains("CONFIGURATION_NOT_FOUND") == true) AuthError.Unavailable else AuthError.Unknown
    else -> AuthError.Unknown
}
