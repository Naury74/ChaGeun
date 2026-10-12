package com.naury.chageun.core.domain.auth

/**
 * 차근 계정에 직접 정하는 부를 이름. Firebase 계정의 표시 이름에 저장하므로 다른 기기에서 로그인해도 같은 이름이다.
 * Google로 다시 로그인해도 이 이름은 덮어쓰지 않는다.
 */
interface ProfileNameRepository {
    /**
     * [name]이 null이면 정한 이름을 지워 Google 이름이나 이메일 앞부분으로 돌아간다.
     * 성공하면 [AuthRepository.currentUser]에 바로 반영된다. 인터넷이 없으면 [AuthError.Network]로 실패한다.
     */
    suspend fun setDisplayName(name: String?): AuthResult
}
