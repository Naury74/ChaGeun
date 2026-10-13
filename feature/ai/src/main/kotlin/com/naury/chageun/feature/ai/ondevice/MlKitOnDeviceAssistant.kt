package com.naury.chageun.feature.ai.ondevice

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.domain.ai.OnDeviceAiException
import com.naury.chageun.core.domain.ai.OnDeviceAiStatus
import com.naury.chageun.core.domain.ai.OnDeviceAssistant
import com.naury.chageun.core.domain.ai.OnDeviceDownload
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile

/**
 * ML Kit GenAI Prompt API로 기기 안 Gemini Nano를 쓴다. 모델은 앱이 아니라 시스템(AICore)이 관리해서
 * 앱 크기가 늘지 않고, 다른 앱과 같은 모델을 함께 쓴다. AICore가 없는 기기와 에뮬레이터는 쓸 수 없다고 본다.
 */
@Singleton
internal class MlKitOnDeviceAssistant @Inject constructor(private val logger: AppLogger) : OnDeviceAssistant {

    // 처음 쓸 때 만든다. 만들기만 해도 Play 서비스에 연결하므로 AI 화면을 열기 전에는 건드리지 않는다.
    private val model: GenerativeModel by lazy { Generation.getClient() }

    @Suppress("TooGenericExceptionCaught") // Play 서비스·AICore가 없으면 여러 예외가 나며 모두 미지원으로 본다.
    override suspend fun status(): OnDeviceAiStatus = try {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> OnDeviceAiStatus.Available
            FeatureStatus.DOWNLOADABLE -> OnDeviceAiStatus.Downloadable
            FeatureStatus.DOWNLOADING -> OnDeviceAiStatus.Downloading
            else -> OnDeviceAiStatus.Unavailable
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn("on_device_ai_status_failed", error = e)
        OnDeviceAiStatus.Unavailable
    }

    override fun download(): Flow<OnDeviceDownload> = flow {
        var total: Long? = null
        model.download().collect { status ->
            when (status) {
                is DownloadStatus.DownloadStarted -> {
                    total = status.bytesToDownload.takeIf { it > 0 }
                    emit(OnDeviceDownload.InProgress(0f))
                }
                is DownloadStatus.DownloadProgress -> emit(
                    OnDeviceDownload.InProgress(
                        total?.let {
                            (status.totalBytesDownloaded.toFloat() / it).coerceIn(0f, 1f)
                        },
                    ),
                )
                is DownloadStatus.DownloadCompleted -> emit(OnDeviceDownload.Completed)
                is DownloadStatus.DownloadFailed -> {
                    logger.warn("on_device_ai_download_failed", error = status.e)
                    emit(OnDeviceDownload.Failed)
                }
                else -> Unit
            }
        }
    }.catch { e ->
        if (e is CancellationException) throw e
        logger.warn("on_device_ai_download_failed", error = e)
        emit(OnDeviceDownload.Failed)
    }

    override fun answer(instruction: String, prompt: String): Flow<String> {
        val request = generateContentRequest(SystemInstruction(instruction), TextPart(prompt)) {
            temperature = TEMPERATURE
            topK = TOP_K
            maxOutputTokens = MAX_OUTPUT_TOKENS
        }
        val builder = StringBuilder()
        return model.generateContentStream(request)
            .transformWhile { response ->
                builder.append(response.candidates.firstOrNull()?.text.orEmpty())
                val cut = cutRepetition(builder.toString())
                if (cut != null) logger.debug("on_device_ai_repetition_cut")
                emit(cut ?: builder.toString())
                cut == null
            }
            .catch { e ->
                if (e is CancellationException) throw e
                logger.warn("on_device_ai_answer_failed", error = e)
                throw OnDeviceAiException(e)
            }
    }

    private companion object {
        // 차 정보에 근거한 답을 원하므로 낮게 둔다.
        const val TEMPERATURE = 0.2f
        const val TOP_K = 10

        // 3~6문장 답에 넉넉한 길이. 길게 두면 반복에 빠졌을 때 오래 기다리게 된다.
        const val MAX_OUTPUT_TOKENS = 320
    }
}
