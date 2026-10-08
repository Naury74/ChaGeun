package com.naury.chageun

import android.app.Application
import androidx.test.runner.AndroidJUnitRunner
import java.io.File

/**
 * 앱이 Room·DataStore를 열기 전에 앱 데이터를 지워 매 테스트를 새로 설치한 상태에서 시작한다.
 *
 * Orchestrator가 테스트마다 프로세스를 새로 띄우므로 이 정리도 테스트마다 한 번씩 실행된다.
 * `pm clear`는 테스트 프로세스 안에서 부를 수 없고, Activity를 띄운 뒤 파일을 지우면 이미 열린 DB와 어긋난다.
 */
class ChageunTestRunner : AndroidJUnitRunner() {

    override fun callApplicationOnCreate(app: Application) {
        val dataDir = app.applicationInfo.dataDir?.let(::File)
        APP_DATA_DIRS.forEach { name -> dataDir?.resolve(name)?.deleteRecursively() }
        super.callApplicationOnCreate(app)
    }

    private companion object {
        // 런타임이 쓰는 code_cache는 앱 데이터가 아니므로 남긴다.
        val APP_DATA_DIRS = listOf("databases", "files", "shared_prefs", "no_backup", "cache")
    }
}
