package com.naury.chageun.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.naury.kinetica.ui.KSegmentedControl

/**
 * iOS의 세그먼트 컨트롤. 회색 트랙 위에서 선택 막대가 늘어났다 줄며 미끄러지고, 고를 때 가볍게 진동한다.
 * 선택지가 2~4개이고 모두 한눈에 보여야 할 때 쓴다. 더 많으면 칩 목록을 쓴다.
 * 큰 글꼴에서는 이름이 두 줄까지 내려가고 막대도 그 높이를 따른다.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    KSegmentedControl(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = { if (it != selectedIndex) onSelect(it) },
        modifier = modifier.fillMaxWidth(),
    )
}
