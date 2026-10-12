package com.naury.chageun.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProfileNameRuleTest {

    @Test
    fun check_trimsAndAcceptsUpToTwentyCharacters() {
        assertThat(ProfileNameRule.check("  차근이  ")).isEqualTo(ProfileNameCheck.Valid("차근이"))
        assertThat(ProfileNameRule.check("가".repeat(20))).isEqualTo(ProfileNameCheck.Valid("가".repeat(20)))
        assertThat(ProfileNameRule.check("가".repeat(21))).isEqualTo(ProfileNameCheck.TooLong)
    }

    @Test
    fun check_countsEmojiAsOneCharacter() {
        val name = "🚗".repeat(20)

        assertThat(ProfileNameRule.length(name)).isEqualTo(20)
        assertThat(ProfileNameRule.check(name)).isEqualTo(ProfileNameCheck.Valid(name))
    }

    @Test
    fun check_blankIsEmpty_andLineBreaksAreRejected() {
        assertThat(ProfileNameRule.check("")).isEqualTo(ProfileNameCheck.Empty)
        assertThat(ProfileNameRule.check("   ")).isEqualTo(ProfileNameCheck.Empty)
        assertThat(ProfileNameRule.check("나우\n리")).isEqualTo(ProfileNameCheck.HasLineBreak)
        assertThat(ProfileNameRule.check("나우\r리")).isEqualTo(ProfileNameCheck.HasLineBreak)
        // 앞뒤 줄바꿈은 공백처럼 지운다.
        assertThat(ProfileNameRule.check("나우리\n")).isEqualTo(ProfileNameCheck.Valid("나우리"))
    }
}
