package com.naury.chageun.update

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 반드시 올려야 하는 버전은 Play Console에 출시할 때 업데이트 우선순위를 [FORCE_PRIORITY] 이상으로 준다(ADR-007).
 * 그러면 앱을 열거나 돌아올 때마다 전체 화면 업데이트를 띄운다. Play에서 설치하지 않은 빌드에서는 조용히 넘어간다.
 */
@Singleton
class AppUpdateChecker @Inject constructor(@ApplicationContext context: Context) {
    private val manager = AppUpdateManagerFactory.create(context)

    fun check(activity: Activity) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            val start = shouldForceUpdate(
                availability = info.updateAvailability(),
                priority = info.updatePriority(),
                isImmediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            )
            if (start) {
                manager.startUpdateFlow(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
            }
        }
    }
}

/** 업데이트를 받다가 앱을 벗어났다면 돌아왔을 때 이어서 띄운다. */
internal fun shouldForceUpdate(availability: Int, priority: Int, isImmediateAllowed: Boolean): Boolean =
    availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ||
        (availability == UpdateAvailability.UPDATE_AVAILABLE && priority >= FORCE_PRIORITY && isImmediateAllowed)

internal const val FORCE_PRIORITY = 4
