package com.naury.chageun.feature.vehicle.album

import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.kinetica.ui.GlassBackdrop
import com.naury.kinetica.ui.glassBackdrop
import com.naury.kinetica.ui.kSurface
import com.naury.kinetica.ui.rememberGlassBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 사진을 크게 보는 화면. 앱 테마와 상관없이 지금 사진을 흐리게 깐 어두운 바탕 위에 사진 카드를 띄운다.
 * 좌우로 넘기면 선택도 따라 바뀌어 두 칸에서 그리드 표시가 맞는다.
 * 사진을 한 번 누르면 조작 막대를 숨기고 사진이 가장자리까지 커지며, 다시 누르면 둥근 카드로 돌아온다.
 *
 * [immersive]는 한 칸 화면처럼 창 전체를 쓰는 경우다. 상태 표시줄 뒤까지 그리고, 뒤로 버튼과 끌어 내려 닫기를 둔다.
 */
@Composable
internal fun PhotoViewer(
    uiState: AlbumUiState,
    photo: AlbumPhoto,
    actions: AlbumActions,
    immersive: Boolean,
    modifier: Modifier = Modifier,
) {
    val photos = uiState.photos
    val pager = rememberPagerState(initialPage = photos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)) {
        photos.size
    }
    FollowSelection(pager, photos, photo, actions.onSelect)
    val scope = rememberCoroutineScope()
    val zoom = remember { PhotoZoomState() }
    LaunchedEffect(pager.currentPage) { zoom.reset() }
    // 확대한 채로 뒤로 가면 먼저 원래 크기로 돌아온다.
    BackHandler(enabled = zoom.isZoomed) { scope.launch { zoom.animateTo(1f, Offset.Zero) } }
    val chrome = rememberChromeVisibility()
    if (immersive) LightStatusBarIcons()
    val notice = when {
        uiState.isCoverUpdated -> ViewerNotice.CoverUpdated
        uiState.isEditFailed -> ViewerNotice.EditFailed
        else -> null
    }
    if (notice != null) {
        LaunchedEffect(notice) {
            delay(NOTICE_MILLIS)
            actions.onDismissMessage()
        }
    }
    val dismiss = rememberDismissDrag(enabled = immersive && !zoom.isZoomed, onDismiss = { actions.onSelect(null) })
    val current = photos.getOrNull(pager.currentPage) ?: photo
    var topBarHeight by remember { mutableIntStateOf(0) }
    var bottomBarHeight by remember { mutableIntStateOf(0) }
    val frame = animateCardFrame(chrome.isShown, topBarHeight, bottomBarHeight, isPeeking = photos.size > 1)
    val backdrop = rememberGlassBackdrop()
    ChageunTheme(darkTheme = true, spacing = ChageunTheme.spacing) {
        Box(modifier.background(Color.Black).clipToBounds()) {
            // 흐린 바탕과 사진 넘김 영역을 기록해 아래 조작 막대가 그 위에서 블러로 비추게 한다. 막대 자신은 기록 밖에 둔다.
            Box(Modifier.fillMaxSize().glassBackdrop(backdrop)) {
                BlurredBackdrop(current, Modifier.graphicsLayer { alpha = 1f - dismiss.progress })
                HorizontalPager(
                    state = pager,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(dismiss.modifier),
                    contentPadding = PaddingValues(horizontal = frame.peek),
                    pageSpacing = PAGE_GAP,
                    key = { photos[it].id },
                ) { page ->
                    // 확대는 지금 사진에만 걸린다. 옆 사진은 넘겨 오는 동안 늘 원래 크기로 보인다.
                    val idle = remember { PhotoZoomState() }
                    ViewerPage(
                        photo = photos[page],
                        frame = frame.card,
                        pageOffset = { (pager.currentPage - page) + pager.currentPageOffsetFraction },
                        zoom = if (page == pager.currentPage) zoom else idle,
                        isCurrent = page == pager.currentPage,
                        showInfo = page == pager.currentPage && chrome.isShown,
                        info = { PhotoInfoOverlay(photos[page], actions) },
                        onTap = chrome.toggle,
                    )
                }
            }
            val chromeAlpha = Modifier.graphicsLayer { alpha = 1f - dismiss.progress * 2f }
            AnimatedVisibility(
                visible = chrome.isShown,
                enter = slideInVertically(motionSpec()) { -it / 2 } + fadeIn(motionSpec()),
                exit = slideOutVertically(motionSpec()) { -it / 2 } + fadeOut(motionSpec()),
                modifier = Modifier.align(Alignment.TopCenter).then(chromeAlpha),
            ) {
                ViewerTopBar(
                    index = pager.currentPage,
                    count = photos.size,
                    onBack = if (immersive) ({ actions.onSelect(null) }) else null,
                    modifier = Modifier
                        .onSizeChanged { topBarHeight = it.height }
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                            ),
                        ),
                )
            }
            ViewerNoticeBadge(
                notice = notice,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = with(LocalDensity.current) { topBarHeight.toDp() }),
            )
            AnimatedVisibility(
                visible = chrome.isShown,
                enter = slideInVertically(motionSpec()) { it / 2 } + fadeIn(motionSpec()),
                exit = slideOutVertically(motionSpec()) { it / 2 } + fadeOut(motionSpec()),
                modifier = Modifier.align(Alignment.BottomCenter).then(chromeAlpha),
            ) {
                ViewerBottomBar(
                    photo = current,
                    photos = photos,
                    index = pager.currentPage,
                    actions = actions,
                    onJump = { scope.launch { pager.animateScrollToPage(it) } },
                    backdrop = backdrop,
                    modifier = Modifier.onSizeChanged { bottomBarHeight = it.height },
                )
            }
            if (immersive) {
                // 두 칸 화면에서는 앨범 화면 아래쪽에 한 번만 띄운다.
                AlbumUndoBar(
                    uiState.undo,
                    actions,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .then(
                            if (chrome.isShown) {
                                Modifier.padding(bottom = with(LocalDensity.current) { bottomBarHeight.toDp() })
                            } else {
                                Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            },
                        ),
                )
            }
        }
    }
}

/** 밖에서 고른 사진으로 넘기고, 손으로 넘겨 멈춘 사진은 선택으로 알린다. */
@Composable
private fun FollowSelection(
    pager: PagerState,
    photos: List<AlbumPhoto>,
    photo: AlbumPhoto,
    onSelect: (String?) -> Unit,
) {
    LaunchedEffect(photo.id) {
        val index = photos.indexOfFirst { it.id == photo.id }
        if (index >= 0 && index != pager.currentPage) pager.scrollToPage(index)
    }
    LaunchedEffect(pager, photos) {
        snapshotFlow { pager.settledPage }.collect { page -> photos.getOrNull(page)?.let { onSelect(it.id) } }
    }
}

@Composable
private fun ViewerBottomBar(
    photo: AlbumPhoto,
    photos: List<AlbumPhoto>,
    index: Int,
    actions: AlbumActions,
    onJump: (Int) -> Unit,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .kSurface(VIEWER_BAR, RectangleShape, backdrop)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(top = ChageunTheme.spacing.xs, bottom = ChageunTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        if (photos.size > 1) ThumbnailStrip(photos, index, onJump)
        ViewerActionRow(photo, actions, Modifier.padding(horizontal = ChageunTheme.spacing.xs))
    }
}

/** 지금 사진을 흐리게 크게 깔아 사진과 어울리는 바탕을 만든다. 흐림을 쓸 수 없는 기기에서는 어두운 단색이다. */
@Composable
private fun BlurredBackdrop(photo: AlbumPhoto, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Crossfade(photo.thumbnailPath, animationSpec = motionSpec(), label = "backdrop") { path ->
                val image by rememberFileImage(path)
                image?.let {
                    Image(
                        it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(BACKDROP_BLUR),
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize().background(BACKDROP_DIM))
    }
}

/** 조작 막대가 보이는지. 처음 열 때는 사진이 먼저 자리 잡은 뒤 조금 늦게 나타난다. */
private class ChromeVisibility(val isShown: Boolean, val toggle: () -> Unit)

@Composable
private fun rememberChromeVisibility(): ChromeVisibility {
    var hasEntered by remember { mutableStateOf(false) }
    var isHiddenByUser by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(CHROME_ENTER_DELAY_MILLIS)
        hasEntered = true
    }
    // TalkBack 사용자는 사진을 눌러 버튼을 다시 꺼낼 수 없으므로 늘 보인다.
    val isShown = (hasEntered && !isHiddenByUser) || rememberTouchExplorationEnabled()
    return ChromeVisibility(isShown) { isHiddenByUser = !isHiddenByUser }
}

/** 카드 자리와 옆 사진이 살짝 보이는 폭. 조작 막대를 숨기면 모두 0으로 줄어 사진이 가장자리까지 커진다. */
private class AnimatedFrame(val card: CardFrame, val peek: Dp)

@Composable
private fun animateCardFrame(isShown: Boolean, topPx: Int, bottomPx: Int, isPeeking: Boolean): AnimatedFrame {
    val density = LocalDensity.current
    val top by animateDpAsState(
        if (isShown) with(density) { topPx.toDp() } else 0.dp,
        motionSpec(),
        label = "card-top",
    )
    val bottom by animateDpAsState(
        if (isShown) with(density) { bottomPx.toDp() } else 0.dp,
        motionSpec(),
        label = "card-bottom",
    )
    val side by animateDpAsState(
        if (isShown && !isPeeking) ChageunTheme.spacing.md else 0.dp,
        motionSpec(),
        label = "card-side",
    )
    val radius by animateDpAsState(
        if (isShown) CARD_RADIUS else 0.dp,
        motionSpec(),
        label = "card-radius",
    )
    val peek by animateDpAsState(
        if (isShown && isPeeking) PEEK else 0.dp,
        motionSpec(),
        label = "card-peek",
    )
    // 스프링이 0 아래로 살짝 넘칠 수 있어 음수 여백이 되지 않게 막는다.
    val frame =
        CardFrame(
            top.coerceAtLeast(0.dp),
            bottom.coerceAtLeast(0.dp),
            side.coerceAtLeast(0.dp),
            radius.coerceAtLeast(0.dp),
        )
    return AnimatedFrame(frame, peek.coerceAtLeast(0.dp))
}

/** 한 칸 화면에서 사진을 아래로 끌어 닫는다. 끄는 만큼 사진이 내려가며 작아지고 바탕이 옅어진다. */
private class DismissDrag(val progress: Float, val modifier: Modifier)

@Composable
private fun rememberDismissDrag(enabled: Boolean, onDismiss: () -> Unit): DismissDrag {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { DISMISS_DISTANCE.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    val state = rememberDraggableState { delta -> offset = (offset + delta).coerceAtLeast(0f) }
    val progress = (offset / (thresholdPx * 2f)).coerceIn(0f, 1f)
    val modifier = Modifier
        .draggable(
            state = state,
            orientation = Orientation.Vertical,
            enabled = enabled,
            onDragStopped = { velocity ->
                if (offset > thresholdPx || velocity > with(density) { DISMISS_VELOCITY.toPx() }) {
                    onDismiss()
                } else {
                    scope.launch { animate(offset, 0f) { value, _ -> offset = value } }
                }
            },
        )
        .graphicsLayer {
            translationY = offset
            val scale = 1f - DISMISS_SHRINK * progress
            scaleX = scale
            scaleY = scale
        }
    return DismissDrag(progress, modifier)
}

/** 어두운 바탕 위에서는 상태 표시줄 아이콘을 흰색으로 바꾸고, 화면을 닫으면 원래대로 돌린다. */
@Composable
private fun LightStatusBarIcons() {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

/** TalkBack처럼 터치 탐색이 켜져 있으면 true. 켜고 끄는 것도 바로 따라간다. */
@Composable
internal fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var enabled by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

private val BACKDROP_BLUR = 30.dp
private val BACKDROP_DIM = Color.Black.copy(alpha = 0.45f)
private val PAGE_GAP = 12.dp
private val PEEK = 40.dp
private val CARD_RADIUS = 24.dp
private val DISMISS_DISTANCE = 120.dp
private val DISMISS_VELOCITY = 1_200.dp
private const val DISMISS_SHRINK = 0.15f
private const val NOTICE_MILLIS = 3_000L
private const val CHROME_ENTER_DELAY_MILLIS = 200L
