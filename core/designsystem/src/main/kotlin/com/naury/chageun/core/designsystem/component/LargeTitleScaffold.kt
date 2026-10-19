package com.naury.chageun.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunGlass
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.kinetica.ui.GlassBackdrop
import com.naury.kinetica.ui.glass
import com.naury.kinetica.ui.glassBackdrop
import com.naury.kinetica.ui.rememberGlassBackdrop
import kotlin.math.roundToInt

/**
 * iOS 탭 화면처럼 큰 제목으로 시작하고, 목록을 올리면 큰 제목이 접히면서 가운데 작은 제목이 나타난다.
 * [title]이 null이면 제목 없이 내용만 보여 준다. 좁은 화면에서 상세를 연 경우처럼 상세가 자기 머리글을 가질 때 쓴다.
 * 위쪽 상태 표시줄 인셋은 앱 셸에서 이미 비우므로 여기서는 더하지 않는다.
 *
 * 내용은 막대 아래까지 그리고, 접힌 막대는 그 아래로 지나가는 목록을 블러로 비추는 글래스가 된다.
 * 아래에 떠 있는 탭 바 높이는 [com.naury.chageun.core.designsystem.theme.LocalBottomBarPadding]로 읽어 목록 끝 여백에 더한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeTitleScaffold(
    title: String?,
    modifier: Modifier = Modifier,
    /** 탭 위에 쌓인 화면처럼 돌아갈 곳이 있을 때 뒤로 버튼을 둔다. */
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop = rememberGlassBackdrop()
    Scaffold(
        modifier = if (title != null) modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (title != null) LargeTitleBar(title, scrollBehavior, backdrop, navigationIcon, actions)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().glassBackdrop(backdrop)) {
            content(padding)
        }
    }
}

/**
 * 작은 제목 바 아래에 큰 제목 줄을 두고, 스크롤한 만큼 큰 제목 줄의 높이를 줄인다.
 * 큰 제목이 사라질수록 가운데 작은 제목이 나타나고, 바 배경은 화면 바탕에서 아래 목록을 비추는 글래스로 바뀐다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LargeTitleBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    backdrop: GlassBackdrop,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val state = scrollBehavior.state
    val collapsed = state.collapsedFraction
    val colors = MaterialTheme.colorScheme
    // 펼쳐 있을 때는 화면 바탕과 같고, 접힐수록 바탕을 걷어 내 아래로 지나가는 목록이 글래스 너머로 비친다.
    val container = colors.background.copy(alpha = 1f - collapsed)
    val separator = colors.outlineVariant
    // 접히면 iOS처럼 바 아래에 얇은 구분선이 나타난다.
    Column(
        Modifier
            .fillMaxWidth()
            .glass(style = ChageunGlass.bar, backdrop = backdrop, intensity = collapsed)
            .drawWithContent {
                drawContent()
                drawLine(
                    color = separator.copy(alpha = separator.alpha * collapsed),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = Dp.Hairline.toPx().coerceAtLeast(1f),
                )
            },
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // 큰 제목이 보이는 동안에는 같은 글이 두 번 읽히지 않도록 접힌 뒤에만 나타낸다.
                    modifier = Modifier.alpha(collapsed),
                )
            },
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = WindowInsets(0),
            // iOS 내비게이션 바 높이에 맞춰 큰 제목 위 빈 줄을 낮춘다.
            expandedHeight = BAR_HEIGHT,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = container, scrolledContainerColor = container),
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground,
            // 'AI에게 물어보기'처럼 긴 제목도 잘리지 않도록 두 줄까지 쓴다.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                // 줄어든 높이 밖으로 글자가 삐져나와 작은 제목 바를 덮지 않게 자른다.
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    // 접힐 수 있는 최대 높이를 큰 제목 줄 높이로 알려 준다.
                    state.heightOffsetLimit = -placeable.height.toFloat()
                    val height = (placeable.height + state.heightOffset).roundToInt().coerceAtLeast(0)
                    layout(placeable.width, height) { placeable.place(0, height - placeable.height) }
                }
                .alpha(1f - collapsed)
                .semantics { heading() }
                .padding(horizontal = ChageunTheme.spacing.gutter, vertical = ChageunTheme.spacing.xs),
        )
    }
}

private val BAR_HEIGHT = 52.dp
