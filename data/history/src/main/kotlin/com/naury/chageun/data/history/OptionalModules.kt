package com.naury.chageun.data.history

import android.content.Context
import com.google.android.gms.common.api.OptionalModuleApi
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Google Play 서비스가 처음 쓸 때 내려받는 ML Kit 모델이 준비될 때까지 기다린다.
 * 매니페스트의 `com.google.mlkit.vision.DEPENDENCIES`는 스토어에서 설치할 때만 미리 받게 하므로,
 * 첫 사용이면 여기서 설치를 요청한다. 요청하지 않으면 첫 호출은 "모듈을 내려받는 중" 오류로 실패한다.
 *
 * 내려받기 시작과 진행을 [onDownloading]으로 알린다. 값은 0~1이고 크기를 아직 모르면 null이다.
 * 이미 받아 둔 경우에는 부르지 않는다.
 *
 * @return 준비되면 true. 설치에 실패하거나 [timeoutMillis] 안에 끝나지 않으면 false다.
 */
internal suspend fun awaitOptionalModule(
    context: Context,
    api: OptionalModuleApi,
    timeoutMillis: Long,
    onDownloading: (Float?) -> Unit,
): Boolean {
    val client = ModuleInstall.getClient(context)
    if (client.areModulesAvailable(api).await().areModulesAvailable()) return true
    onDownloading(null)
    return withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine { continuation ->
            lateinit var listener: InstallStatusListener
            fun finish(ready: Boolean) {
                client.unregisterListener(listener)
                if (continuation.isActive) continuation.resume(ready)
            }
            listener = InstallStatusListener { update ->
                when (update.installState) {
                    InstallState.STATE_COMPLETED -> finish(true)
                    InstallState.STATE_FAILED, InstallState.STATE_CANCELED -> finish(false)
                    else ->
                        update.progressInfo
                            ?.takeIf { it.totalBytesToDownload > 0 }
                            ?.let { onDownloading(it.bytesDownloaded.toFloat() / it.totalBytesToDownload) }
                }
            }
            client.installModules(ModuleInstallRequest.newBuilder().addApi(api).setListener(listener).build())
                .addOnSuccessListener { if (it.areModulesAlreadyInstalled()) finish(true) }
                .addOnFailureListener { finish(false) }
            continuation.invokeOnCancellation { client.unregisterListener(listener) }
        }
    } ?: false
}
