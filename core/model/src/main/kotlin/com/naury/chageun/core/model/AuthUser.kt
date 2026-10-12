package com.naury.chageun.core.model

/** 차근 계정에 로그인한 방법. 같은 이메일이면 둘 다 연결될 수 있다. */
enum class AuthMethod { Email, Google }

/**
 * 로그인한 사용자. 계정은 클라우드 백업에만 쓰므로 이메일·UID와 화면에 보일 이름·사진 주소 말고는 들고 있지 않는다.
 *
 * [displayName]은 차근 계정 자체의 이름이다. 앱이 Google 이름으로 덮어쓰지 않으므로 사용자가 정한 이름이면 그대로 남는다.
 * [googleName]과 [photoUrl]은 Google로 로그인할 때 Google 계정에서 받아 온 값이다.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean,
    val methods: Set<AuthMethod>,
    val googleName: String? = null,
    val photoUrl: String? = null,
) {
    /** Google 계정은 이미 인증된 이메일이라 이메일 가입만 확인 메일이 필요하다. */
    val needsEmailVerification: Boolean get() = !isEmailVerified && AuthMethod.Google !in methods

    /** 화면에서 부를 이름. 계정에 정한 이름, Google 이름, 이메일 @ 앞부분 순서로 고른다. */
    val profileName: String?
        get() = displayName.nonBlank()
            ?: googleName.nonBlank()
            ?: email?.substringBefore('@').nonBlank()
}

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
