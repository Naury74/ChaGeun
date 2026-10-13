package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.ai.OnDeviceAiException
import com.naury.chageun.core.domain.ai.OnDeviceAiStatus
import com.naury.chageun.core.domain.ai.OnDeviceAssistant
import com.naury.chageun.core.domain.ai.OnDeviceDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * [reply]를 단어 단위로 흘려보내는 기기 안 AI. [failAnswer]가 참이면 답 대신 실패한다.
 * 마지막으로 받은 지시문과 질문 글은 [lastInstruction], [lastPrompt]에 남는다.
 */
class FakeOnDeviceAssistant(var status: OnDeviceAiStatus = OnDeviceAiStatus.Available) : OnDeviceAssistant {
    var reply = "Check the engine oil within 500 km."
    var failAnswer = false
    var downloadSucceeds = true
    var lastInstruction: String? = null
    var lastPrompt: String? = null
    var downloads = 0
        private set

    override suspend fun status() = status

    override fun download(): Flow<OnDeviceDownload> = flow {
        downloads++
        emit(OnDeviceDownload.InProgress(HALF))
        if (downloadSucceeds) {
            status = OnDeviceAiStatus.Available
            emit(OnDeviceDownload.Completed)
        } else {
            emit(OnDeviceDownload.Failed)
        }
    }

    override fun answer(instruction: String, prompt: String): Flow<String> = flow {
        lastInstruction = instruction
        lastPrompt = prompt
        if (failAnswer) throw OnDeviceAiException()
        val words = reply.split(" ")
        words.indices.forEach { emit(words.take(it + 1).joinToString(" ")) }
    }
}

private const val HALF = 0.5f
