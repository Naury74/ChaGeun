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
 * "Get notified" 액션을 반환한다. 시스템 권한 요청은 사용자가 여기서 동의한 뒤에만 띄우며,
 * 응답과 관계없이 온보딩을 마친다.
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
