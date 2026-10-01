package com.naury.chageun.core.security

/** Encrypts short sensitive values (plate number, VIN) before they are written to local storage. */
interface FieldCipher {
    fun encrypt(plaintext: String): String

    /** @throws java.security.GeneralSecurityException when the payload was tampered with or the key changed. */
    fun decrypt(ciphertext: String): String
}
