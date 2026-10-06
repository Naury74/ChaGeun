package com.naury.chageun.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold

/** 설정 아래 화면들은 탭 위에 쌓이므로 큰 제목과 뒤로 버튼을 함께 둔다. */
@Composable
internal fun SettingsScaffold(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    LargeTitleScaffold(
        title = stringResource(titleRes),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
            }
        },
        content = content,
    )
}
