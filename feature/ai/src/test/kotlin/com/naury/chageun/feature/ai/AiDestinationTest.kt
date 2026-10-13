package com.naury.chageun.feature.ai

import com.google.common.truth.Truth.assertThat
import java.net.URLDecoder
import org.junit.Test

class AiDestinationTest {

    @Test
    fun chatGptAndClaude_getPrefilledQuestion() {
        val prompt = "차량: Kia Sportage\n질문: 엔진오일 언제 갈아?"

        val chatGpt = externalLaunchFor(AiDestination.ChatGpt, prompt) as ExternalLaunch.Prefilled
        val claude = externalLaunchFor(AiDestination.Claude, prompt) as ExternalLaunch.Prefilled

        assertThat(chatGpt.url).startsWith("https://chatgpt.com/?q=")
        assertThat(claude.url).startsWith("https://claude.ai/new?q=")
        assertThat(URLDecoder.decode(chatGpt.url.substringAfter("q="), "UTF-8")).isEqualTo(prompt)
        // 공백이 +로 바뀌면 일부 사이트가 그대로 보여 주므로 %20을 쓴다.
        assertThat(chatGpt.url).doesNotContain("+")
    }

    @Test
    fun gemini_andTooLongPrompts_copyThenOpen() {
        assertThat(
            externalLaunchFor(AiDestination.Gemini, "hi"),
        ).isEqualTo(ExternalLaunch.CopyThenOpen("https://gemini.google.com/app"))

        val long = "가".repeat(MAX_PREFILL_URL_LENGTH)
        assertThat(
            externalLaunchFor(AiDestination.ChatGpt, long),
        ).isEqualTo(ExternalLaunch.CopyThenOpen("https://chatgpt.com/"))
    }

    @Test
    fun otherApp_usesShareSheet() {
        assertThat(externalLaunchFor(AiDestination.OtherApp, "hi")).isEqualTo(ExternalLaunch.Share)
    }
}
