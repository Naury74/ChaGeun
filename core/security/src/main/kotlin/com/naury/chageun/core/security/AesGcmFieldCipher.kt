package com.naury.chageun.core.security

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/**
 * AES-256-GCM with a random IV per value. Payload layout: `version(1) | iv(12) | ciphertext+tag`.
 * The version byte lets a future key rotation decrypt values written by older builds.
 */
internal class AesGcmFieldCipher @Inject constructor(private val keyProvider: SecretKeyProvider) : FieldCipher {

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreateKey())
        val iv = cipher.iv
        val sealed = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(1 + iv.size + sealed.size)
            .put(FORMAT_VERSION)
            .put(iv)
            .put(sealed)
            .array()
        return Base64.getEncoder().encodeToString(payload)
    }

    override fun decrypt(ciphertext: String): String {
        val payload = ByteBuffer.wrap(Base64.getDecoder().decode(ciphertext))
        if (payload.get() != FORMAT_VERSION) throw GeneralSecurityException("Unsupported payload version")
        val iv = ByteArray(IV_LENGTH_BYTES).also(payload::get)
        val sealed = ByteArray(payload.remaining()).also(payload::get)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider.getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(sealed), Charsets.UTF_8)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FORMAT_VERSION: Byte = 1
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
    }
}
