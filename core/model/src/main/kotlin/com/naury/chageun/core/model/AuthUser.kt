package com.naury.chageun.core.model

/** 차근 계정에 로그인한 방법. 같은 이메일이면 둘 다 연결될 수 있다. */
enum class AuthMethod { Email, Google }

/** 로그인한 사용자. 계정은 클라우드 백업에만 쓰므로 이메일·UID 말고는 들고 있지 않는다. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean,
    val methods: Set<AuthMethod>,
) {
    /** Google 계정은 이미 인증된 이메일이라 이메일 가입만 확인 메일이 필요하다. */
    val needsEmailVerification: Boolean get() = !isEmailVerified && AuthMethod.Google !in methods
}
