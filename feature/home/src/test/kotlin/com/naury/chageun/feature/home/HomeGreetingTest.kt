package com.naury.chageun.feature.home

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import java.time.LocalTime
import org.junit.Test

class HomeGreetingTest {

    @Test
    fun dayPart_followsHourBoundaries() {
        assertThat(DayPart.of(LocalTime.of(4, 59))).isEqualTo(DayPart.Night)
        assertThat(DayPart.of(LocalTime.of(5, 0))).isEqualTo(DayPart.Morning)
        assertThat(DayPart.of(LocalTime.of(12, 0))).isEqualTo(DayPart.Afternoon)
        assertThat(DayPart.of(LocalTime.of(18, 0))).isEqualTo(DayPart.Evening)
        assertThat(DayPart.of(LocalTime.of(22, 0))).isEqualTo(DayPart.Night)
    }

    @Test
    fun greetingName_prefersDisplayName_thenEmailId() {
        val google = AuthUser("u", "naury@gmail.com", " 나우리 ", true, setOf(AuthMethod.Google))
        val email = AuthUser("u", "driver.kim@example.com", null, true, setOf(AuthMethod.Email))
        val unverified = email.copy(isEmailVerified = false)

        assertThat(google.greetingName()).isEqualTo("나우리")
        assertThat(email.greetingName()).isEqualTo("driver.kim")
        assertThat(unverified.greetingName()).isNull()
    }
}
