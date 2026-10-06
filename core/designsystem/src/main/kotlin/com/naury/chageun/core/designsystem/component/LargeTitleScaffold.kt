package com.naury.chageun.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * iOS 탭 화면처럼 큰 제목으로 시작하고, 목록을 올리면 작은 제목 바로 접힌다.
 * [title]이 null이면 제목 없이 내용만 보여 준다. 좁은 화면에서 상세를 연 경우처럼 상세가 자기 머리글을 가질 때 쓴다.
 * 위쪽 상태 표시줄 인셋은 앱 셸에서 이미 비우므로 여기서는 더하지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeTitleScaffold(
    title: String?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = if (title != null) modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else modifier,
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (title != null) {
                LargeTopAppBar(
                    title = { Text(title, modifier = Modifier.semantics { heading() }) },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background,
                        scrolledContainerColor = colors.surface,
                    ),
                )
            }
        },
        content = content,
    )
}
