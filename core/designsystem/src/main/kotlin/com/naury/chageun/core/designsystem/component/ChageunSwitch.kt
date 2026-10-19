package com.naury.chageun.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.naury.kinetica.ui.KSwitch

/**
 * 켜고 끄는 스위치. 누르는 동안 손잡이가 늘어나고 출렁이며 넘어간다.
 * 행 전체를 `toggleable`로 누르게 할 때는 [onCheckedChange]를 null로 두어 스위치는 표시만 한다.
 */
@Composable
fun ChageunSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    KSwitch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
}
