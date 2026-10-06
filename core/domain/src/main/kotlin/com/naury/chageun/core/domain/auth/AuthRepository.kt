package com.naury.chageun.core.domain.auth

import com.naury.chageun.core.model.AuthUser
import kotlinx.coroutines.flow.Flow

/** 화면에 따로 안내하는 인증 실패. 나머지는 [Unknown]으로 묶는다. */
enum class AuthError {
    Network,
    EmailInUse,
    WeakPassword,
    InvalidCredentials,
    TooManyRequests,
    UserDisabled,
    NoGoogleAccount,
    Unavailable,
    Unknown,
}

sealed interface AuthResult {
    /** [isNewUser]가 참이면 이번에 가입한 것이다. 분석 이벤트를 sign_up과 login으로 나눌 때 쓴다. */
    data class Success(val isNewUser: Boolean = false) : AuthResult

    data class Failure(val error: AuthError) : AuthResult
}

/** Google 계정 선택 결과. 토큰을 받아 오는 일은 화면(Activity)이 하고 로그인은 [AuthRepository]가 한다. */
sealed interface GoogleIdTokenResult {
    data class Token(val idToken: String) : GoogleIdTokenResult

    /** 사용자가 계정 선택 창을 닫았다. 따로 안내하지 않는다. */
    data object Cancelled : GoogleIdTokenResult

    data object NoAccount : GoogleIdTokenResult

    /** 이 빌드에 Google 로그인용 OAuth 클라이언트가 없다. */
    data object Unavailable : GoogleIdTokenResult

    data object Failed : GoogleIdTokenResult
}

interface AuthRepository {
    /** 로그인하지 않았으면 null. 인증 메일 확인 같은 계정 상태 변화도 흘려보낸다. */
    val currentUser: Flow<AuthUser?>

    /** 가입하면 바로 로그인되고 인증 메일을 보낸다. */
    suspend fun signUpWithEmail(email: String, password: String): AuthResult

    suspend fun signInWithEmail(email: String, password: String): AuthResult

    /** 처음 쓰는 Google 계정이면 가입까지 한 번에 끝난다. */
    suspend fun signInWithGoogle(idToken: String): AuthResult

    /** 가입하지 않은 이메일이어도 성공으로 돌려준다. 가입 여부를 알려 주지 않기 위해서다. */
    suspend fun sendPasswordReset(email: String): AuthResult

    suspend fun sendEmailVerification(): AuthResult

    /** 서버에서 계정 상태를 다시 읽는다. 다른 앱에서 인증 링크를 누른 뒤 확인할 때 쓴다. */
    suspend fun reload(): AuthResult

    /** 로그아웃해도 기기에 있는 차량·기록은 그대로 둔다. */
    suspend fun signOut()
}
