package com.naury.chageun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.naury.chageun.core.notification.DeepLink
import com.naury.chageun.core.notification.DeepLinks.deepLinkOrNull
import com.naury.chageun.ui.ChageunRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val deepLink = mutableStateOf<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A restored activity already navigated for its launch intent.
        if (savedInstanceState == null) deepLink.value = intent.deepLinkOrNull()
        setContent {
            ChageunRoot(deepLink = deepLink.value, onDeepLinkHandled = { deepLink.value = null })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.deepLinkOrNull()?.let { deepLink.value = it }
    }
}
