package com.naury.chageun.core.common.logging

/**
 * 로그를 남기는 단일 진입점이다. 이벤트는 변하지 않는 snake_case [event] 이름으로 구분한다.
 *
 * Throwable은 클래스 이름만 남기고 메시지는 버린다. 플랫폼과 제공처 예외 메시지에 요청 데이터가
 * 들어 있는 경우가 많기 때문이다.
 */
interface AppLogger {
    fun debug(event: String, vararg fields: LogField)

    fun warn(event: String, vararg fields: LogField, error: Throwable? = null)

    fun error(event: String, vararg fields: LogField, error: Throwable? = null)
}

fun formatLogLine(event: String, fields: Array<out LogField>, error: Throwable?): String = buildString {
    append(event)
    fields.forEach { append(' ').append(it.key).append('=').append(it.value) }
    error?.let { append(" exception=").append(it::class.java.simpleName) }
}
