package com.naury.chageun.feature.ai

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

enum class AiProvider(val packageName: String?, val label: String?) {
    ChatGpt("com.openai.chatgpt", "ChatGPT"),
    Claude("com.anthropic.claude", "Claude"),
    Gemini("com.google.android.apps.bard", "Gemini"),
    Other(null, null),
}

/** Shares [text] to the chosen AI app, or the system chooser when it is not installed or "other" is picked. */
internal fun shareToAi(context: Context, provider: AiProvider, text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val target = provider.packageName?.takeIf { context.isInstalled(it) }
    val intent = if (target != null) send.setPackage(target) else Intent.createChooser(send, chooserTitle)
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.isInstalled(packageName: String): Boolean = try {
    packageManager.getPackageInfo(packageName, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}
