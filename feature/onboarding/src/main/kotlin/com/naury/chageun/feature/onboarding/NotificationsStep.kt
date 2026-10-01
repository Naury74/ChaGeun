package com.naury.chageun.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
internal fun NotificationsStep() {
    Text(stringResource(R.string.onboarding_notifications_title), style = MaterialTheme.typography.headlineSmall)
    Text(
        stringResource(R.string.onboarding_notifications_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Returns the action for "Get notified". The system prompt is shown only after the user opts in here,
 * and onboarding finishes regardless of the answer.
 */
@Composable
internal fun rememberNotificationOptIn(onFinish: () -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onFinish() }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onFinish()
        }
    }
}
