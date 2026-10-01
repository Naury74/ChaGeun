package com.naury.chageun.core.common.logging

/**
 * The only values that may be attached to a log event.
 *
 * Free-form strings are deliberately not accepted so plate numbers, owner names, notes
 * or raw provider payloads cannot reach logs or crash reports by accident.
 */
sealed interface LogField {
    val key: String
    val value: String

    data class RequestId(override val value: String) : LogField {
        override val key = "request_id"
    }

    data class ProviderCode(override val value: String) : LogField {
        override val key = "provider_code"
    }

    data class ErrorType(val type: String) : LogField {
        override val key = "error_type"
        override val value = type
    }

    data class DurationMs(val millis: Long) : LogField {
        override val key = "duration_ms"
        override val value = millis.toString()
    }

    data class SchemaVersion(val version: Int) : LogField {
        override val key = "schema_version"
        override val value = version.toString()
    }

    data class Success(val isSuccess: Boolean) : LogField {
        override val key = "success"
        override val value = isSuccess.toString()
    }
}
