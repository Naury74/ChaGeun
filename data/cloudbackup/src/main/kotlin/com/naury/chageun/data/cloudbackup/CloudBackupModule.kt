package com.naury.chageun.data.cloudbackup

import android.content.Context
import android.os.Build
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
import java.util.UUID

@Module
@InstallIn(SingletonComponent::class)
internal interface AutoBackupModule {
    @Binds
    fun bindScheduler(scheduler: PeriodicAutoBackupScheduler): AutoBackupScheduler
}

@Module
@InstallIn(SingletonComponent::class)
internal object CloudBackupModule {

    @Provides
    fun provideEnvironment(@ApplicationContext context: Context, clock: Clock) = CloudBackupEnvironment(
        // 백업 ZIP은 올린 뒤 바로 지우고, 복원 파일은 다음 복원 때 덮어쓴다. 시스템이 정리해도 되는 캐시에 둔다.
        workDirectory = File(context.cacheDir, "cloud-backup"),
        appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
        deviceModel = Build.MODEL,
        clock = clock,
        newBackupId = { UUID.randomUUID().toString() },
    )

    // 개발자 비용이 생기지 않도록 Firebase 저장소(Firestore·Storage)는 쓰지 않는다(ADR-006).
    // 사용자 본인 Google 드라이브 연결을 붙이기 전까지는 백업을 쓸 수 없다고 안내한다.
    @Provides
    fun provideRepository(): CloudBackupRepository = UnavailableCloudBackupRepository
}
