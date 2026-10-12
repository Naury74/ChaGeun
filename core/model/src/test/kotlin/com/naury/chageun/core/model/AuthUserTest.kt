package com.naury.chageun.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthUserTest {

    private val google = AuthUser(
        uid = "u",
        email = "naury@gmail.com",
        displayName = null,
        isEmailVerified = true,
        methods = setOf(AuthMethod.Google),
        googleName = "나우리",
    )

    @Test
    fun profileName_prefersAccountName_overGoogleName() {
        val named = google.copy(displayName = " 차근이 ")

        assertThat(named.profileName).isEqualTo("차근이")
    }

    @Test
    fun profileName_usesGoogleName_whenAccountHasNoName() {
        assertThat(google.profileName).isEqualTo("나우리")
        assertThat(google.copy(displayName = "  ").profileName).isEqualTo("나우리")
    }

    @Test
    fun profileName_fallsBackToEmailId_thenNothing() {
        val email = AuthUser("u", "driver.kim@example.com", null, true, setOf(AuthMethod.Email))

        assertThat(email.profileName).isEqualTo("driver.kim")
        assertThat(email.copy(email = null).profileName).isNull()
        assertThat(google.copy(googleName = " ", email = null).profileName).isNull()
    }
}
