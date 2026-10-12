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

    @Test
    fun profileName_userSetName_winsOverGoogleName_andResetFallsBack() {
        val named = google.copy(displayName = "차근이")

        assertThat(named.profileName).isEqualTo("차근이")
        // 정한 이름을 지우면 Google 이름, Google 이름도 없으면 이메일 앞부분으로 돌아간다.
        assertThat(named.copy(displayName = null).profileName).isEqualTo("나우리")
        assertThat(named.copy(displayName = null, googleName = null).profileName).isEqualTo("naury")
    }
}
