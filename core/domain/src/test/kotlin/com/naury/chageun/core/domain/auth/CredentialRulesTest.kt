package com.naury.chageun.core.domain.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CredentialRulesTest {

    @Test
    fun email_needsLocalPartDomainAndDot() {
        assertThat(CredentialRules.isValidEmail(" driver@example.com ")).isTrue()
        assertThat(CredentialRules.isValidEmail("driver@example")).isFalse()
        assertThat(CredentialRules.isValidEmail("driver example@example.com")).isFalse()
        assertThat(CredentialRules.isValidEmail("")).isFalse()
    }

    @Test
    fun password_needsLengthLetterAndDigit() {
        assertThat(CredentialRules.isStrongPassword("chageun1")).isTrue()
        assertThat(CredentialRules.isStrongPassword("chage1")).isFalse()
        assertThat(CredentialRules.isStrongPassword("12345678")).isFalse()
        assertThat(CredentialRules.isStrongPassword("abcdefgh")).isFalse()
    }
}
