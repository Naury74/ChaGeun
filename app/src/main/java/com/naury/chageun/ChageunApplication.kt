package com.naury.chageun

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.naury.chageun.core.notification.ReminderScheduler
import com.naury.chageun.data.cloudbackup.AutoBackupSync
import com.naury.chageun.data.weather.KoreaRegionUpdateScheduler
import com.naury.chageun.logging.UsageStatsSync
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class ChageunApplication :
    Application(),
    Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var usageStatsSync: UsageStatsSync

    @Inject lateinit var autoBackupSync: AutoBackupSync

    @Inject lateinit var regionUpdateScheduler: KoreaRegionUpdateScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // 누끼(`:cutout`)·글자 인식(`:ocr`)과 영수증 글자 인식(`:framekit_ocr`) 프로세스도 이 onCreate를 거친다.
        // 그곳에서는 알림 예약·통계 동기화·자동 백업·지역 목록 확인 예약을 하지 않는다.
        if (!isMainProcess(currentProcessName(), packageName)) return
        reminderScheduler.schedule()
        usageStatsSync.start(appScope)
        autoBackupSync.start(appScope)
        regionUpdateScheduler.schedule()
    }

    private fun currentProcessName(): String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        getProcessName()
    } else {
        runCatching { File("/proc/self/cmdline").readText().trimEnd('\u0000') }.getOrNull()
    }
}

/**
 * 앱의 기본 프로세스인지. 분리 프로세스 이름은 `패키지:이름`이다. 이름을 읽지 못하면 기본 프로세스로 보고
 * 초기화를 한다. 알림 예약을 빠뜨리는 것보다 한 번 더 하는 편이 낫다.
 */
internal fun isMainProcess(processName: String?, packageName: String): Boolean =
    processName.isNullOrEmpty() || processName == packageName
