package com.naury.chageun.feature.vehicle

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.naury.chageun.core.designsystem.component.ChageunButton
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant

/** 줄 끝이나 카드 아래에 놓는 글자 버튼. 눈에 보이는 크기는 작아도 누르는 영역은 48dp를 지킨다. */
@Composable
internal fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: KTone = KTone.Accent,
    trailingIcon: ImageVector? = null,
    fullWidth: Boolean = false,
) {
    ChageunButton(
        text,
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        variant = KVariant.Text,
        tone = tone,
        trailingIcon = trailingIcon,
        fullWidth = fullWidth,
    )
}
