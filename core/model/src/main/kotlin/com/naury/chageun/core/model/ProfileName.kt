package com.naury.chageun.core.model

/** 사용자가 직접 정하는 부를 이름을 확인한 결과. */
sealed interface ProfileNameCheck {
    /** 앞뒤 공백을 뺀 [name]을 그대로 저장하면 된다. */
    data class Valid(val name: String) : ProfileNameCheck

    /** 비워 두면 정한 이름을 지우고 Google 이름이나 이메일 앞부분으로 돌아간다. */
    data object Empty : ProfileNameCheck

    data object TooLong : ProfileNameCheck

    data object HasLineBreak : ProfileNameCheck
}

/** 부를 이름 규칙. 홈 인사말 한 줄에 들어가도록 20자까지, 한 줄로만 받는다. */
object ProfileNameRule {
    const val MAX_LENGTH = 20

    fun check(input: String): ProfileNameCheck {
        val name = input.trim()
        return when {
            name.isEmpty() -> ProfileNameCheck.Empty
            name.any { it == '\n' || it == '\r' } -> ProfileNameCheck.HasLineBreak
            length(name) > MAX_LENGTH -> ProfileNameCheck.TooLong
            else -> ProfileNameCheck.Valid(name)
        }
    }

    /** 이모지처럼 두 칸을 쓰는 글자도 한 글자로 센다. */
    fun length(input: String): Int = input.trim().let { it.codePointCount(0, it.length) }
}
