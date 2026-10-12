package com.naury.chageun

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.CustomTestApplication
import dagger.hilt.components.SingletonComponent

/**
 * 흐름 테스트의 앱. Hilt 테스트 앱이라 테스트가 모듈을 바꾸고([com.naury.chageun.di.FixedWeatherModule]) 저장소를 주입받는다.
 *
 * 실제 앱처럼 WorkManager는 Hilt 작업 팩토리로 띄운다. 알림 예약·사용 통계·자동 백업은 흐름과 상관없고 네트워크를 쓸 수
 * 있어 시작하지 않는다.
 */
open class ChageunTestApplicationBase :
    Application(),
    Configuration.Provider {

    // 앱 컴포넌트는 테스트마다 HiltAndroidRule이 만들므로, WorkManager를 처음 쓸 때 꺼낸다.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                EntryPointAccessors.fromApplication(this, WorkerFactoryEntryPoint::class.java).workerFactory(),
            )
            .build()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerFactoryEntryPoint {
    fun workerFactory(): HiltWorkerFactory
}

@CustomTestApplication(ChageunTestApplicationBase::class)
interface ChageunTestApplication
