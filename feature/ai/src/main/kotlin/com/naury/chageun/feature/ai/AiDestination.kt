package com.naury.chageun.feature.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.naury.chageun.core.domain.analytics.AiTarget
import java.net.URLEncoder

/** 질문을 받을 AI. [OnDevice]만 차근 안에서 답하고, 나머지는 사용자의 계정으로 각 서비스에서 이어서 쓴다. */
enum class AiDestination(val analyticsTarget: AiTarget) {
    OnDevice(AiTarget.OnDevice),
    ChatGpt(AiTarget.ChatGpt),
    Claude(AiTarget.Claude),
    Gemini(AiTarget.Gemini),
    OtherApp(AiTarget.Other),
}

/** 외부 AI를 여는 방법. 앱 안에 남의 채팅 화면을 넣지 않고 브라우저 탭이나 공유로 넘긴다. */
internal sealed interface ExternalLaunch {
    /** 질문을 채운 주소를 앱 안 브라우저 탭으로 연다. 로그인은 사용자의 브라우저 것을 그대로 쓴다. */
    data class Prefilled(val url: String) : ExternalLaunch

    /** 주소로 질문을 넘길 수 없을 때. 질문을 복사해 두고 연다. */
    data class CopyThenOpen(val url: String) : ExternalLaunch

    /** 시스템 공유로 설치된 AI 앱을 고르게 한다. */
    data object Share : ExternalLaunch
}

/**
 * ChatGPT·Claude는 주소의 q 값으로 질문을 받는다(공식 API가 아니어서 바뀔 수 있다).
 * Gemini는 이런 주소가 없어서 복사 후 연다. 주소가 너무 길면 브라우저나 서버가 자를 수 있어 복사로 바꾼다.
 */
internal fun externalLaunchFor(destination: AiDestination, prompt: String): ExternalLaunch {
    val query = URLEncoder.encode(prompt, Charsets.UTF_8.name()).replace("+", "%20")
    return when (destination) {
        AiDestination.ChatGpt -> prefilledOrCopy("$CHATGPT_URL?q=$query", CHATGPT_URL)
        AiDestination.Claude -> prefilledOrCopy("$CLAUDE_URL?q=$query", CLAUDE_URL)
        AiDestination.Gemini -> ExternalLaunch.CopyThenOpen(GEMINI_URL)
        AiDestination.OtherApp, AiDestination.OnDevice -> ExternalLaunch.Share
    }
}

private fun prefilledOrCopy(prefilled: String, base: String): ExternalLaunch = if (prefilled.length <=
    MAX_PREFILL_URL_LENGTH
) {
    ExternalLaunch.Prefilled(prefilled)
} else {
    ExternalLaunch.CopyThenOpen(base)
}

/** [prompt]를 [destination]으로 넘긴다. 브라우저가 없으면 호출한 쪽의 launchExternal이 안내한다. */
internal fun openExternally(context: Context, destination: AiDestination, prompt: String, texts: ExternalTexts) {
    when (val launch = externalLaunchFor(destination, prompt)) {
        is ExternalLaunch.Prefilled -> openTab(context, launch.url)
        is ExternalLaunch.CopyThenOpen -> {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText(texts.clipLabel, prompt))
            Toast.makeText(context, texts.copied, Toast.LENGTH_LONG).show()
            openTab(context, launch.url)
        }
        ExternalLaunch.Share -> {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, prompt)
            }
            context.startActivity(
                Intent.createChooser(send, texts.chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

/** 화면 문구는 Compose에서 만들어 넘긴다. */
internal data class ExternalTexts(val chooserTitle: String, val clipLabel: String, val copied: String)

private fun openTab(context: Context, url: String) {
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .build()
        .launchUrl(context, Uri.parse(url))
}

private const val CHATGPT_URL = "https://chatgpt.com/"
private const val CLAUDE_URL = "https://claude.ai/new"
private const val GEMINI_URL = "https://gemini.google.com/app"

/** 대부분의 브라우저와 서버가 받아 주는 주소 길이보다 넉넉히 짧게 둔다. */
internal const val MAX_PREFILL_URL_LENGTH = 6_000
