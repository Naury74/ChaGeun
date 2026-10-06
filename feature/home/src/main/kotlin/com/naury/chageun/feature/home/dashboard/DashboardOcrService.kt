package com.naury.chageun.feature.home.dashboard

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
import androidx.core.net.toUri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.naury.chageun.core.domain.mileage.DashboardTextReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 계기판 글자 인식을 앱과 다른 프로세스(`:ocr`)에서 실행한다. ML Kit의 TFLite 네이티브 코드가 일부 CPU
 * (예: Android 16 arm64 에뮬레이터)에서 SIGILL로 죽어도 이 프로세스만 끝나고, 앱은 직접 입력으로 돌아간다.
 * 숫자와 영문 단위만 필요해 라틴 모델을 쓴다.
 */
internal class DashboardOcrService : Service() {

    private val thread = HandlerThread("dashboard-ocr").apply { start() }

    private val messenger = Messenger(
        object : Handler(thread.looper) {
            override fun handleMessage(msg: Message) {
                val lines = msg.data.getString(KEY_IMAGE)?.let { image -> runCatching { recognize(image) }.getOrNull() }
                val reply = Message.obtain(null, MSG_RESULT).apply {
                    data = Bundle().apply { lines?.let { putStringArrayList(KEY_LINES, ArrayList(it)) } }
                }
                msg.replyTo?.send(reply)
            }
        },
    )

    /** 한 줄로 끊겨 "ODO"와 숫자가 떨어져도 판독 규칙이 문맥을 볼 수 있게 블록 전체도 한 줄로 함께 넘긴다. */
    private fun recognize(image: String): List<String> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val text = Tasks.await(recognizer.process(InputImage.fromFilePath(this, image.toUri())))
            text.textBlocks.flatMap { block -> block.lines.map { it.text } + block.text.replace('\n', ' ') }
        } finally {
            recognizer.close()
        }
    }

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
        const val MSG_READ = 1
        const val MSG_RESULT = 2
        const val KEY_IMAGE = "image"
        const val KEY_LINES = "lines"
    }
}

/** [DashboardOcrService]에 연결해 결과를 기다린다. 프로세스가 죽거나 너무 오래 걸리면 null이다. */
internal class IsolatedDashboardTextReader @Inject constructor(@ApplicationContext private val context: Context) :
    DashboardTextReader {

    override suspend fun read(imageUri: String): List<String>? = withTimeoutOrNull(TIMEOUT_MILLIS) { request(imageUri) }

    private suspend fun request(imageUri: String): List<String>? = suspendCancellableCoroutine { continuation ->
        val connection = object : ServiceConnection {
            val replies = Messenger(
                object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        if (msg.what == DashboardOcrService.MSG_RESULT) {
                            finish(msg.data.getStringArrayList(DashboardOcrService.KEY_LINES))
                        }
                    }
                },
            )

            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val request = Message.obtain(null, DashboardOcrService.MSG_READ).apply {
                    data = Bundle().apply { putString(DashboardOcrService.KEY_IMAGE, imageUri) }
                    replyTo = replies
                }
                runCatching { Messenger(binder).send(request) }.onFailure { finish(null) }
            }

            // 인식 도중 네이티브 코드가 죽으면 여기로 온다.
            override fun onServiceDisconnected(name: ComponentName) = finish(null)

            override fun onBindingDied(name: ComponentName) = finish(null)

            fun finish(lines: List<String>?) {
                if (continuation.isActive) {
                    runCatching { context.unbindService(this) }
                    continuation.resume(lines)
                }
            }
        }
        val bound = context.bindService(
            Intent(context, DashboardOcrService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        if (!bound) {
            continuation.resume(null)
        } else {
            continuation.invokeOnCancellation { runCatching { context.unbindService(connection) } }
        }
    }

    private companion object {
        // 모델을 처음 내려받는 경우까지 고려한 시간. 넘으면 직접 입력으로 돌아간다.
        const val TIMEOUT_MILLIS = 30_000L
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DashboardModule {
    @Binds
    fun bindDashboardTextReader(reader: IsolatedDashboardTextReader): DashboardTextReader
}
