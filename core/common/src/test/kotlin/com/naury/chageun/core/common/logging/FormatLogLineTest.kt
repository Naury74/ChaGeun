package com.naury.chageun.core.common.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatLogLineTest {

    @Test
    fun formatsEventWithFields() {
        val line = formatLogLine(
            event = "vehicle_lookup_completed",
            fields = arrayOf(LogField.RequestId("req_1"), LogField.DurationMs(420), LogField.Success(true)),
            error = null,
        )

        assertThat(line).isEqualTo("vehicle_lookup_completed request_id=req_1 duration_ms=420 success=true")
    }

    @Test
    fun dropsExceptionMessage_andKeepsTypeOnly() {
        val line = formatLogLine(
            event = "vehicle_lookup_failed",
            fields = emptyArray(),
            error = IllegalStateException("plate 12가3456 owner Hong"),
        )

        assertThat(line).isEqualTo("vehicle_lookup_failed exception=IllegalStateException")
        assertThat(line).doesNotContain("12가3456")
    }
}
