package com.naury.chageun.core.security

/** 짧은 민감 값(차량번호, VIN)을 로컬 저장소에 쓰기 전에 암호화한다. */
interface FieldCipher {
    fun encrypt(plaintext: String): String

    /** @throws java.security.GeneralSecurityException payload가 변조됐거나 키가 바뀐 경우. */
    fun decrypt(ciphertext: String): String
}
