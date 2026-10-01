package com.naury.chageun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.notification.DeepLinks.maintenanceItemOrNull
import com.naury.chageun.ui.ChageunRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val deepLinkItem = mutableStateOf<MaintenanceItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A restored activity already navigated for its launch intent.
        if (savedInstanceState == null) deepLinkItem.value = intent.maintenanceItemOrNull()
        setContent {
            ChageunRoot(deepLinkItem = deepLinkItem.value, onDeepLinkHandled = { deepLinkItem.value = null })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.maintenanceItemOrNull()?.let { deepLinkItem.value = it }
    }
}
