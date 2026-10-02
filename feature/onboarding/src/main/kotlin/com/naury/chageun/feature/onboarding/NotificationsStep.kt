package com.naury.chageun.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.ItemIconBadge

@Composable
internal fun NotificationsStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
    ) {
        ItemIconBadge(Icons.Filled.NotificationsActive, ChageunTheme.colors.good, size = 72.dp)
        Text(
            stringResource(R.string.onboarding_notifications_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.onboarding_notifications_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    // 어떤 알림이 오는지 미리 보여 주면 권한 요청이 덜 갑작스럽다.
    val colors = ChageunTheme.colors
    listOf(
        Triple(Icons.Filled.OilBarrel, colors.upcoming, R.string.onboarding_notify_example_oil),
        Triple(Icons.Filled.FactCheck, colors.critical, R.string.onboarding_notify_example_inspection),
        Triple(Icons.Filled.Speed, colors.unknown, R.string.onboarding_notify_example_mileage),
    ).forEach { (icon, tone, textRes) ->
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(ChageunTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                ItemIconBadge(icon, tone, size = 36.dp)
                Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
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
