package com.naury.chageun.core.domain.auth

/** 가입·로그인 입력 검사. 서버도 검사하지만 보내기 전에 알려 주려고 같은 기준을 둔다. */
object CredentialRules {
    const val MIN_PASSWORD_LENGTH = 8

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(email: String): Boolean = emailPattern.matches(email.trim())

    /** 8자 이상이고 글자와 숫자가 모두 들어가야 한다. */
    fun isStrongPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH &&
        password.any { it.isLetter() } &&
        password.any { it.isDigit() }
}
