package com.naury.chageun.data.cloudbackup

import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CloudBackupModule {

    @Provides
    fun provideRemote(logger: AppLogger): CloudBackupRemote = FirebaseCloudBackupRemote(
        firestore = { FirebaseFirestore.getInstance() },
        storage = { FirebaseStorage.getInstance() },
        logger = logger,
    )

    @Provides
    fun provideEnvironment(@ApplicationContext context: Context, clock: Clock) = CloudBackupEnvironment(
        // 백업 ZIP은 올린 뒤 바로 지우고, 복원 파일은 다음 복원 때 덮어쓴다. 시스템이 정리해도 되는 캐시에 둔다.
        workDirectory = File(context.cacheDir, "cloud-backup"),
        appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
        deviceModel = Build.MODEL,
        clock = clock,
        // Storage 규칙은 영문·숫자·_·-만 받는다.
        newBackupId = { UUID.randomUUID().toString() },
    )

    @Provides
    @Singleton
    fun provideRepository(
        @ApplicationContext context: Context,
        firebase: Lazy<DefaultCloudBackupRepository>,
    ): CloudBackupRepository = if (FirebaseApp.getApps(context).isEmpty()) {
        UnavailableCloudBackupRepository
    } else {
        firebase.get()
    }
}
