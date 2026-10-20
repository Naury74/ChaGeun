package com.naury.chageun.core.domain.ai

import kotlinx.coroutines.flow.Flow

/** 기기 안 AI 모델의 준비 상태. */
enum class OnDeviceAiStatus {
    Available,

    /** 기기는 지원하지만 모델을 먼저 내려받아야 한다. */
    Downloadable,
    Downloading,

    /** 이 기기나 Android 버전에서는 쓸 수 없다. */
    Unavailable,
}

sealed interface OnDeviceDownload {
    /** [progress]는 0~1. 전체 크기를 모르면 null. */
    data class InProgress(val progress: Float?) : OnDeviceDownload

    data object Completed : OnDeviceDownload

    data object Failed : OnDeviceDownload
}

/**
 * 기기 안에서 돌아가는 AI(Gemini Nano). 질문과 차 정보가 기기 밖으로 나가지 않고 비용도 없다.
 * 답변은 기기 성능과 모델 크기 때문에 서버 AI보다 짧고 단순할 수 있다.
 */
interface OnDeviceAssistant {
    suspend fun status(): OnDeviceAiStatus

    fun download(): Flow<OnDeviceDownload>

    /**
     * [instruction]은 답변 방식, [prompt]는 차 정보와 대화. 지금까지 만든 답을 누적해서 흘려보낸다.
     * 실패하면 [OnDeviceAiException]을 던진다.
     */
    fun answer(instruction: String, prompt: String): Flow<String>
}

class OnDeviceAiException(cause: Throwable? = null) : Exception(cause)
