package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KEmptyState
import com.naury.kinetica.ui.KVariant

/**
 * 목록이 비었을 때 무엇을 하면 채워지는지 알려 주는 화면. 행동이 없으면 버튼을 그리지 않는다.
 * 그림은 Kinetica 빈 화면처럼 원판 위에서 천천히 떠 있고, Reduce Motion에서는 멈춰 있다.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    tone: ToneColors = ChageunTheme.colors.unknown,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        KEmptyState(
            title = title,
            message = body,
            art = { Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(ART_ICON)) },
            action = if (actionLabel != null && onAction != null) {
                {
                    KButton(
                        text = actionLabel,
                        onClick = onAction,
                        modifier = Modifier.heightIn(min = ChageunTheme.spacing.minTouchTarget),
                        variant = KVariant.Tonal,
                    )
                }
            } else {
                null
            },
        )
    }
}

private val ART_ICON = 48.dp
