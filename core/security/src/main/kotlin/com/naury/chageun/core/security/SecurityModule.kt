package com.naury.chageun.core.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SecurityModule {
    @Binds
    @Singleton
    fun bindSecretKeyProvider(provider: AndroidKeystoreKeyProvider): SecretKeyProvider

    @Binds
    @Singleton
    fun bindFieldCipher(cipher: AesGcmFieldCipher): FieldCipher
}
