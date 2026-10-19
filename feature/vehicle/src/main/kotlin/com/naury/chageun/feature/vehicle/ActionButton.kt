package com.naury.chageun.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KSurface
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.KineticaTheme
import com.naury.kinetica.ui.LocalKineticaContentColor
import com.naury.kinetica.ui.jellyPress

/**
 * Kinetica 버튼. 평소에는 [KButton]을 그대로 쓴다. KButton은 이름을 한 줄로만 그려 글자를 아주 크게 키우면 잘리므로,
 * 그때는 같은 모양과 색의 표면에 줄을 바꿔 이름을 다 보여 준다. 누를 때 출렁이는 것도 같다.
 */
@Composable
internal fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KVariant = KVariant.Filled,
    tone: KTone = KTone.Accent,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    shape: Shape = KineticaTheme.design.shapes.let { it.shape(it.button) },
) {
    if (LocalDensity.current.fontScale < WRAP_FONT_SCALE) {
        KButton(
            text,
            onClick = onClick,
            modifier = modifier,
            variant = variant,
            tone = tone,
            icon = icon,
            trailingIcon = trailingIcon,
            loading = loading,
            fullWidth = fullWidth,
            shape = shape,
        )
        return
    }
    KSurface(
        onClick = onClick,
        modifier = modifier
            .then(if (loading) Modifier else Modifier.jellyPress())
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = ChageunTheme.spacing.minTouchTarget),
        shape = shape,
        enabled = !loading,
        variant = variant,
        tone = tone,
    ) {
        val ink = LocalKineticaContentColor.current
        Row(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Icon(it, contentDescription = null, tint = ink, modifier = Modifier.size(ICON_SIZE)) }
            Text(
                text,
                color = ink,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
            trailingIcon?.let { Icon(it, contentDescription = null, tint = ink, modifier = Modifier.size(ICON_SIZE)) }
        }
    }
}

// 이보다 글자를 키우면 한 줄 버튼 이름이 좁은 화면에서 잘리기 시작한다.
private const val WRAP_FONT_SCALE = 1.3f
private val ICON_SIZE = 18.dp
