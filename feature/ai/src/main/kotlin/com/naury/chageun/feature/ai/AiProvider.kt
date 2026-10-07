package com.naury.chageun.feature.ai

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.naury.chageun.core.domain.analytics.AiTarget

enum class AiProvider(val packageName: String?, val label: String?) {
    ChatGpt("com.openai.chatgpt", "ChatGPT"),
    Claude("com.anthropic.claude", "Claude"),
    Gemini("com.google.android.apps.bard", "Gemini"),
    Other(null, null),
}

internal val AiProvider.analyticsTarget: AiTarget
    get() = when (this) {
        AiProvider.ChatGpt -> AiTarget.ChatGpt
        AiProvider.Claude -> AiTarget.Claude
        AiProvider.Gemini -> AiTarget.Gemini
        AiProvider.Other -> AiTarget.Other
    }

/** [text]를 선택한 AI 앱으로 공유한다. 앱이 설치되어 있지 않거나 "other"를 고르면 시스템 선택기를 띄운다. */
internal fun shareToAi(context: Context, provider: AiProvider, text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val target = provider.packageName?.takeIf { context.isInstalled(it) }
    val intent = if (target != null) send.setPackage(target) else Intent.createChooser(send, chooserTitle)
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** 패키지 가시성 선언에 넣어 둔 AI 앱 중 기기에 설치된 것. */
internal fun installedAiProviders(context: Context): Set<AiProvider> =
    AiProvider.entries.filter { provider -> provider.packageName?.let(context::isInstalled) == true }.toSet()

private fun Context.isInstalled(packageName: String): Boolean = try {
    packageManager.getPackageInfo(packageName, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}
