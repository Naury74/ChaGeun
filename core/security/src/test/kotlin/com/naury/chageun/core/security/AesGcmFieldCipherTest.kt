package com.naury.chageun.core.security

import com.google.common.truth.Truth.assertThat
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.KeyGenerator
import org.junit.Assert.assertThrows
import org.junit.Test

class AesGcmFieldCipherTest {

    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = AesGcmFieldCipher { key }

    @Test
    fun roundTripsKoreanPlate() {
        val encrypted = cipher.encrypt("서울12가3456")

        assertThat(encrypted).doesNotContain("3456")
        assertThat(cipher.decrypt(encrypted)).isEqualTo("서울12가3456")
    }

    @Test
    fun usesFreshIvForEveryValue() {
        assertThat(cipher.encrypt("123가4567")).isNotEqualTo(cipher.encrypt("123가4567"))
    }

    @Test
    fun rejectsTamperedPayload() {
        val bytes = Base64.getDecoder().decode(cipher.encrypt("123가4567"))
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()

        assertThrows(GeneralSecurityException::class.java) {
            cipher.decrypt(Base64.getEncoder().encodeToString(bytes))
        }
    }

    @Test
    fun rejectsDifferentKey() {
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val encrypted = cipher.encrypt("123가4567")

        assertThrows(GeneralSecurityException::class.java) {
            AesGcmFieldCipher { otherKey }.decrypt(encrypted)
        }
    }

    @Test
    fun rejectsUnknownPayloadVersion() {
        val bytes = Base64.getDecoder().decode(cipher.encrypt("123가4567"))
        bytes[0] = 9

        assertThrows(GeneralSecurityException::class.java) {
            cipher.decrypt(Base64.getEncoder().encodeToString(bytes))
        }
    }
}
