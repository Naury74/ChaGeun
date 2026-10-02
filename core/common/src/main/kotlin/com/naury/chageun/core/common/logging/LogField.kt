package com.naury.chageun.core.common.logging

/**
 * 로그 이벤트에 붙일 수 있는 값은 이것뿐이다.
 *
 * 자유 형식 문자열은 일부러 받지 않는다. 차량번호, 소유자 이름, 메모나 제공처 원본 응답이
 * 실수로 로그나 크래시 리포트에 들어가지 않게 하기 위해서다.
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
