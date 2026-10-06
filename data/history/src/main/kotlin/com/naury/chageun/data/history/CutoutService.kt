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
 */
internal class CutoutService : Service() {

    private val thread = HandlerThread("cutout").apply { start() }

    private val messenger = Messenger(
        object : Handler(thread.looper) {
            override fun handleMessage(msg: Message) {
                val source = msg.data.getString(KEY_SOURCE)
                val target = msg.data.getString(KEY_TARGET)
                val succeeded = source != null &&
                    target != null &&
                    runBlocking {
                        runCatching { MlKitSubjectCutter.cutout(File(source), File(target)) }.getOrDefault(false)
                    }
                msg.replyTo?.send(Message.obtain(null, MSG_RESULT, if (succeeded) 1 else 0, 0))
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
        const val KEY_SOURCE = "source"
        const val KEY_TARGET = "target"
    }
}

/** [CutoutService]에 연결해 결과를 기다린다. 프로세스가 죽거나 너무 오래 걸리면 실패로 본다. */
internal class IsolatedSubjectCutter @Inject constructor(@ApplicationContext private val context: Context) :
    SubjectCutter {

    override suspend fun cutout(source: File, target: File): Boolean =
        withTimeoutOrNull(TIMEOUT_MILLIS) { request(source, target) } ?: false

    private suspend fun request(source: File, target: File): Boolean = suspendCancellableCoroutine { continuation ->
        val connection = object : ServiceConnection {
            val replies = Messenger(
                object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        if (msg.what == CutoutService.MSG_RESULT) finish(msg.arg1 == 1)
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
                runCatching { Messenger(binder).send(request) }.onFailure { finish(false) }
            }

            // 분리 도중 네이티브 코드가 죽으면 여기로 온다.
            override fun onServiceDisconnected(name: ComponentName) = finish(false)

            override fun onBindingDied(name: ComponentName) = finish(false)

            fun finish(succeeded: Boolean) {
                if (continuation.isActive) {
                    runCatching { context.unbindService(this) }
                    continuation.resume(succeeded)
                }
            }
        }
        val bound = context.bindService(
            Intent(context, CutoutService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        if (!bound) {
            continuation.resume(false)
        } else {
            continuation.invokeOnCancellation { runCatching { context.unbindService(connection) } }
        }
    }

    private companion object {
        // 모델을 처음 내려받는 경우까지 고려한 시간. 넘으면 원본 사진을 쓴다.
        const val TIMEOUT_MILLIS = 60_000L
    }
}
