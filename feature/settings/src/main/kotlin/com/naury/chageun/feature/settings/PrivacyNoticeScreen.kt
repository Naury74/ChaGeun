package com.naury.chageun.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme

/** 앱이 실제로 저장하고 전송하는 내용과 항상 일치해야 한다. 권한이나 SDK를 새로 추가하면 함께 수정한다. */
@Composable
fun PrivacyNoticeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsScaffold(R.string.settings_privacy, onBack, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ChageunTheme.spacing.gutter)
                .padding(bottom = ChageunTheme.spacing.lg)
                .widthIn(max = CONTENT_MAX_WIDTH),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.lg),
        ) {
            NOTICE_SECTIONS.forEach { (title, body) -> NoticeSection(title, body) }
        }
    }
}

@Composable
private fun NoticeSection(@StringRes titleRes: Int, @StringRes bodyRes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(bodyRes), style = MaterialTheme.typography.bodyMedium)
    }
}

private val NOTICE_SECTIONS = listOf(
    R.string.privacy_stored_title to R.string.privacy_stored_body,
    R.string.privacy_transfer_title to R.string.privacy_transfer_body,
    R.string.privacy_security_title to R.string.privacy_security_body,
    R.string.privacy_control_title to R.string.privacy_control_body,
    R.string.privacy_disclaimer_title to R.string.privacy_disclaimer_body,
)

private val CONTENT_MAX_WIDTH = 640.dp
