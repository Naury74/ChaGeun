package com.naury.chageun.core.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.storage.PROFILE_PHOTO_DIRECTORY
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.ProfilePhotoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthModule {
    @Binds
    fun bindGoogleIdTokenSource(source: CredentialGoogleIdTokenSource): GoogleIdTokenSource

    @Binds
    fun bindGoogleDriveAccess(access: PlayServicesGoogleDriveAccess): GoogleDriveAccess

    @Binds
    fun bindProfilePhotoDownloader(downloader: HttpProfilePhotoDownloader): ProfilePhotoDownloader

    @Binds
    fun bindProfilePhotoRepository(repository: CachedProfilePhotoRepository): ProfilePhotoRepository

    companion object {
        @Provides
        @Singleton
        fun provideAuthRepository(
            @ApplicationContext context: Context,
            logger: AppLogger,
            profilePhotos: ProfilePhotoCache,
            @Dispatcher(ChageunDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        ): AuthRepository = if (FirebaseApp.getApps(context).isEmpty()) {
            UnavailableAuthRepository
        } else {
            FirebaseAuthRepository(FirebaseAuth.getInstance(), context, logger, profilePhotos, ioDispatcher)
        }

        @Provides
        @Singleton
        fun provideProfilePhotoCache(@ApplicationContext context: Context): ProfilePhotoCache =
            ProfilePhotoCache(File(context.filesDir, PROFILE_PHOTO_DIRECTORY))
    }
}
