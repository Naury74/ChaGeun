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
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.naury.chageun.core.domain.mileage.DashboardTextReader
import com.naury.chageun.core.domain.mileage.TextReadResult
import com.naury.chageun.feature.home.dashboard.DashboardOcrService.Companion.downloadProgress
import com.naury.chageun.feature.home.dashboard.DashboardOcrService.Companion.toResult
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
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
                val replyTo = msg.replyTo ?: return
                val result = msg.data.getString(KEY_IMAGE)?.let { image ->
                    runCatching {
                        runBlocking {
                            recognize(image) { progress -> runCatching { replyTo.send(downloadingMessage(progress)) } }
                        }
                    }.getOrNull()
                } ?: TextReadResult.Unavailable
                replyTo.send(result.toMessage())
            }
        },
    )

    /** 한 줄로 끊겨 "ODO"와 숫자가 떨어져도 판독 규칙이 문맥을 볼 수 있게 블록 전체도 한 줄로 함께 넘긴다. */
    private suspend fun recognize(image: String, onDownloading: (Float?) -> Unit): TextReadResult {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            if (!awaitOptionalModule(this, recognizer, MODULE_TIMEOUT_MILLIS, onDownloading)) {
                return TextReadResult.ModelUnavailable
            }
            val text = recognizer.process(InputImage.fromFilePath(this, image.toUri())).await()
            TextReadResult.Read(
                text.textBlocks.flatMap { block -> block.lines.map { it.text } + block.text.replace('\n', ' ') },
            )
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
        const val MSG_DOWNLOADING = 3
        const val KEY_IMAGE = "image"
        const val KEY_LINES = "lines"

        private const val RESULT_READ = 0
        private const val RESULT_MODEL_UNAVAILABLE = 1
        private const val RESULT_UNAVAILABLE = 2
        private const val PROGRESS_SCALE = 1000
        private const val PROGRESS_UNKNOWN = -1

        // 모델은 보통 몇 초면 받아지지만 느린 네트워크를 고려한다. 진행률을 보여 주므로 넉넉히 기다린다.
        private const val MODULE_TIMEOUT_MILLIS = 150_000L

        fun downloadingMessage(progress: Float?): Message = Message.obtain(
            null,
            MSG_DOWNLOADING,
            progress?.let { (it * PROGRESS_SCALE).toInt() } ?: PROGRESS_UNKNOWN,
            0,
        )

        fun Message.downloadProgress(): Float? = arg1.takeIf { it >= 0 }?.let { it.toFloat() / PROGRESS_SCALE }

        fun TextReadResult.toMessage(): Message = when (this) {
            is TextReadResult.Read -> Message.obtain(null, MSG_RESULT, RESULT_READ, 0).apply {
                data = Bundle().apply { putStringArrayList(KEY_LINES, ArrayList(lines)) }
            }
            TextReadResult.ModelUnavailable -> Message.obtain(null, MSG_RESULT, RESULT_MODEL_UNAVAILABLE, 0)
            TextReadResult.Unavailable -> Message.obtain(null, MSG_RESULT, RESULT_UNAVAILABLE, 0)
        }

        fun Message.toResult(): TextReadResult = when (arg1) {
            RESULT_READ -> data.getStringArrayList(KEY_LINES)?.let { TextReadResult.Read(it) }
                ?: TextReadResult.Unavailable
            RESULT_MODEL_UNAVAILABLE -> TextReadResult.ModelUnavailable
            else -> TextReadResult.Unavailable
        }
    }
}

/** [DashboardOcrService]에 연결해 결과를 기다린다. 프로세스가 죽거나 너무 오래 걸리면 [TextReadResult.Unavailable]이다. */
internal class IsolatedDashboardTextReader @Inject constructor(@ApplicationContext private val context: Context) :
    DashboardTextReader {

    override suspend fun read(imageUri: String, onDownloadingModel: (Float?) -> Unit): TextReadResult =
        withTimeoutOrNull(TIMEOUT_MILLIS) { request(imageUri, onDownloadingModel) } ?: TextReadResult.Unavailable

    private suspend fun request(imageUri: String, onDownloadingModel: (Float?) -> Unit): TextReadResult =
        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                val replies = Messenger(
                    object : Handler(Looper.getMainLooper()) {
                        override fun handleMessage(msg: Message) {
                            when (msg.what) {
                                DashboardOcrService.MSG_DOWNLOADING ->
                                    if (continuation.isActive) onDownloadingModel(msg.downloadProgress())
                                DashboardOcrService.MSG_RESULT -> finish(msg.toResult())
                            }
                        }
                    },
                )

                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val request = Message.obtain(null, DashboardOcrService.MSG_READ).apply {
                        data = Bundle().apply { putString(DashboardOcrService.KEY_IMAGE, imageUri) }
                        replyTo = replies
                    }
                    runCatching { Messenger(binder).send(request) }.onFailure { finish(TextReadResult.Unavailable) }
                }

                // 인식 도중 네이티브 코드가 죽으면 여기로 온다.
                override fun onServiceDisconnected(name: ComponentName) = finish(TextReadResult.Unavailable)

                override fun onBindingDied(name: ComponentName) = finish(TextReadResult.Unavailable)

                fun finish(result: TextReadResult) {
                    if (continuation.isActive) {
                        runCatching { context.unbindService(this) }
                        continuation.resume(result)
                    }
                }
            }
            val bound = context.bindService(
                Intent(context, DashboardOcrService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
            if (!bound) {
                continuation.resume(TextReadResult.Unavailable)
            } else {
                continuation.invokeOnCancellation { runCatching { context.unbindService(connection) } }
            }
        }

    private companion object {
        // 모델을 처음 내려받는 경우까지 고려한 시간. 넘으면 직접 입력으로 돌아간다.
        const val TIMEOUT_MILLIS = 180_000L
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DashboardModule {
    @Binds
    fun bindDashboardTextReader(reader: IsolatedDashboardTextReader): DashboardTextReader
}
