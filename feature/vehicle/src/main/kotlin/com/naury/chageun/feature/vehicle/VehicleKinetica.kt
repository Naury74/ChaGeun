package com.naury.chageun.feature.vehicle

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.naury.kinetica.render.animateNumberAsState
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant
import kotlin.math.roundToLong

/** 주행거리가 바뀌면 이전 값에서 새 값까지 숫자가 올라가며 바뀐다. 처음 그릴 때는 바로 보여 준다. */
@Composable
internal fun rollingMileage(km: Long?): Long? = km?.let { animateNumberAsState(it.toDouble()).value.roundToLong() }

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
    ActionButton(
        text,
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        variant = KVariant.Text,
        tone = tone,
        trailingIcon = trailingIcon,
        fullWidth = fullWidth,
    )
}
