package com.naury.chageun.core.common.logging

/**
 * Single logging entry point. Events are identified by a stable snake_case [event] name.
 *
 * Throwables are reduced to their class name; messages are dropped because platform and provider
 * exceptions frequently embed request data.
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
