package com.naury.chageun.core.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.domain.auth.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthModule {
    @Binds
    fun bindGoogleIdTokenSource(source: CredentialGoogleIdTokenSource): GoogleIdTokenSource

    @Binds
    fun bindGoogleDriveAccess(access: PlayServicesGoogleDriveAccess): GoogleDriveAccess

    companion object {
        @Provides
        @Singleton
        fun provideAuthRepository(@ApplicationContext context: Context, logger: AppLogger): AuthRepository =
            if (FirebaseApp.getApps(context).isEmpty()) {
                UnavailableAuthRepository
            } else {
                FirebaseAuthRepository(FirebaseAuth.getInstance(), context, logger)
            }
    }
}
