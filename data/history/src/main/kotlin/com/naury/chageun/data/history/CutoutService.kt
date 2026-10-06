package com.naury.chageun.data.history

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.data.history.CutoutService.Companion.toStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 피사체 분리를 앱과 다른 프로세스(`:cutout`)에서 실행한다. 네이티브 코드가 죽어도 이 프로세스만 끝나고,
 * 앱은 연결이 끊긴 것을 보고 원본 사진을 그대로 쓴다.
 * 모델 내려받기와 처리 시작은 [MSG_STATUS]로, 결과는 [MSG_RESULT]로 앱에 알린다.
 */
internal class CutoutService : Service() {

    private val thread = HandlerThread("cutout").apply { start() }

    private val cutter by lazy { MlKitSubjectCutter(this) }

    private val messenger = Messenger(
        object : Handler(thread.looper) {
            override fun handleMessage(msg: Message) {
                val replyTo = msg.replyTo ?: return
                val source = msg.data.getString(KEY_SOURCE)
                val target = msg.data.getString(KEY_TARGET)
                val result = if (source == null || target == null) {
                    CutoutResult.Failed
                } else {
                    runBlocking {
                        runCatching {
                            cutter.cutout(File(source), File(target)) { status ->
                                runCatching { replyTo.send(status.toMessage()) }
                            }
                        }.getOrDefault(CutoutResult.Failed)
                    }
                }
                replyTo.send(Message.obtain(null, MSG_RESULT, result.ordinal, 0))
            }
        },
    )

    override fun onCreate() {
        super.onCreate()
        // ML Kit은 기본 프로세스의 ContentProvider에서만 초기화된다. 이 프로세스에서는 직접 초기화한다.
        MlKitContext.initializeIfNeeded(this)
    }

    override fun onBind(intent: Intent): IBinder = messenger.binder

    override fun onDestroy() {
        thread.quitSafely()
        super.onDestroy()
    }

    companion object {
        const val MSG_CUTOUT = 1
        const val MSG_RESULT = 2
        const val MSG_STATUS = 3
        const val KEY_SOURCE = "source"
        const val KEY_TARGET = "target"

        private const val STATUS_DOWNLOADING = 0
        private const val STATUS_PROCESSING = 1
        private const val PROGRESS_SCALE = 1000
        private const val PROGRESS_UNKNOWN = -1

        // Message의 정수 두 칸에 상태 종류와 천분율 진행률을 담는다.
        fun CutoutStatus.toMessage(): Message = when (this) {
            is CutoutStatus.DownloadingModel -> Message.obtain(
                null,
                MSG_STATUS,
                STATUS_DOWNLOADING,
                progress?.let { (it * PROGRESS_SCALE).toInt() } ?: PROGRESS_UNKNOWN,
            )
            else -> Message.obtain(null, MSG_STATUS, STATUS_PROCESSING, 0)
        }

        fun Message.toStatus(): CutoutStatus = if (arg1 == STATUS_DOWNLOADING) {
            CutoutStatus.DownloadingModel(arg2.takeIf { it >= 0 }?.let { it.toFloat() / PROGRESS_SCALE })
        } else {
            CutoutStatus.Processing
        }
    }
}

/** [CutoutService]에 연결해 결과를 기다린다. 프로세스가 죽거나 너무 오래 걸리면 실패로 본다. */
internal class IsolatedSubjectCutter @Inject constructor(@ApplicationContext private val context: Context) :
    SubjectCutter {

    override suspend fun cutout(source: File, target: File, onStatus: (CutoutStatus) -> Unit): CutoutResult =
        withTimeoutOrNull(TIMEOUT_MILLIS) { request(source, target, onStatus) } ?: CutoutResult.Failed

    private suspend fun request(source: File, target: File, onStatus: (CutoutStatus) -> Unit): CutoutResult =
        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                val replies = Messenger(
                    object : Handler(Looper.getMainLooper()) {
                        override fun handleMessage(msg: Message) {
                            when (msg.what) {
                                CutoutService.MSG_STATUS -> if (continuation.isActive) onStatus(msg.toStatus())
                                CutoutService.MSG_RESULT -> finish(
                                    CutoutResult.entries.getOrElse(msg.arg1) { CutoutResult.Failed },
                                )
                            }
                        }
                    },
                )

                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val request = Message.obtain(null, CutoutService.MSG_CUTOUT).apply {
                        data = Bundle().apply {
                            putString(CutoutService.KEY_SOURCE, source.path)
                            putString(CutoutService.KEY_TARGET, target.path)
                        }
                        replyTo = replies
                    }
                    runCatching { Messenger(binder).send(request) }.onFailure { finish(CutoutResult.Failed) }
                }

                // 분리 도중 네이티브 코드가 죽으면 여기로 온다.
                override fun onServiceDisconnected(name: ComponentName) = finish(CutoutResult.Failed)

                override fun onBindingDied(name: ComponentName) = finish(CutoutResult.Failed)

                fun finish(result: CutoutResult) {
                    if (continuation.isActive) {
                        runCatching { context.unbindService(this) }
                        continuation.resume(result)
                    }
                }
            }
            val bound = context.bindService(
                Intent(context, CutoutService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
            if (!bound) {
                continuation.resume(CutoutResult.Failed)
            } else {
                continuation.invokeOnCancellation { runCatching { context.unbindService(connection) } }
            }
        }

    private companion object {
        // 모델을 처음 내려받는 경우까지 고려한 시간. 넘으면 원본 사진을 쓰고 다시 시도를 안내한다.
        const val TIMEOUT_MILLIS = 180_000L
    }
}
