package com.naury.chageun.core.security

import javax.crypto.SecretKey

fun interface SecretKeyProvider {
    fun getOrCreateKey(): SecretKey
}
