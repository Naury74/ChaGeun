package com.naury.chageun.feature.vehicle.album

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.feature.vehicle.R
import java.io.File

/**
 * 사진 보기의 동영상 페이지. 처음에는 포스터와 재생 버튼만 보이고, 재생을 누르면 그 자리에서 재생한다.
 * 확대는 하지 않고, 화면을 한 번 누르면 사진처럼 조작 막대를 숨기거나 꺼낸다.
 * 다른 페이지로 넘기거나 화면을 떠나면 플레이어를 놓고 포스터로 돌아간다. 다시 재생하면 멈춘 곳부터 이어 본다.
 *
 * @param overlay 날짜·메모. 영상 위, 재생 버튼 아래에 그린다.
 */
@Composable
internal fun VideoPageContent(
    photo: AlbumPhoto,
    isCurrent: Boolean,
    showControls: Boolean,
    onTap: () -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
) {
    var isStarted by rememberSaveable(photo.id) { mutableStateOf(false) }
    // 편집해서 파일이 바뀌면 길이도 달라지므로 처음부터 본다.
    var positionMs by rememberSaveable(photo.id, photo.filePath) { mutableLongStateOf(0L) }
    LaunchedEffect(isCurrent) { if (!isCurrent) isStarted = false }
    val latestOnTap by rememberUpdatedState(onTap)
    val description = mediaDescription(photo)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onTap = { latestOnTap() }) }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (isStarted && isCurrent) {
            VideoPlayback(
                photo = photo,
                startAtMs = positionMs,
                showControls = showControls,
                onStopped = { position ->
                    positionMs = position
                    if (position == 0L) isStarted = false
                },
                overlay = overlay,
            )
        } else {
            Box(Modifier.videoFrame(photo)) {
                VideoPoster(photo)
                VideoDurationBadge(photo.durationMs, Modifier.align(Alignment.TopEnd).padding(BADGE_INSET))
            }
            overlay()
            PlayPauseButton(
                isPlaying = false,
                onClick = { isStarted = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(CONTROL_INSET),
            )
        }
    }
}

/**
 * 재생 중인 동영상. 첫 장면이 나올 때까지는 포스터를 그대로 덮어 두어 깜빡이지 않는다. 카드가 영상보다 높으면
 * 포스터처럼 위아래를 검게 남기고 영상은 자르지 않는다.
 * 조작 막대가 보이거나 멈춰 있으면 오른쪽 아래 재생·일시 정지 버튼을, 조작 막대가 보이면 위쪽 재생 시간을 보여 준다.
 *
 * @param onStopped 플레이어를 놓을 때 멈춘 위치를 알린다. 끝까지 봤으면 0이다.
 */
// ContentFrame·재생 시간 상태는 Media3에서 아직 안정 API가 아니다. FrameKit과 같은 버전으로 고정해 쓴다.
@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayback(
    photo: AlbumPhoto,
    startAtMs: Long,
    showControls: Boolean,
    onStopped: (Long) -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val player = remember(photo.filePath) { albumPlayer(context, photo.filePath, startAtMs) }
    var playWhenReady by remember(player) { mutableStateOf(true) }
    val latestOnStopped by rememberUpdatedState(onStopped)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(value: Boolean, reason: Int) {
                playWhenReady = value
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) latestOnStopped(0L)
            }
        }
        player.addListener(listener)
        onDispose {
            if (player.playbackState != Player.STATE_ENDED) latestOnStopped(player.currentPosition)
            player.removeListener(listener)
            player.release()
        }
    }
    // 앱을 잠시 떠나면 소리가 계속 나지 않게 멈춘다. 돌아와서 재생은 사용자가 고른다.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }
    Box(Modifier.fillMaxSize()) {
        ContentFrame(
            player = player,
            modifier = Modifier.fillMaxSize(),
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            contentScale = ContentScale.Fit,
            shutter = {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Box(Modifier.videoFrame(photo)) { VideoPoster(photo) }
                }
            },
        )
        overlay()
        AnimatedVisibility(
            visible = showControls || !playWhenReady,
            enter = fadeIn(motionSpec()),
            exit = fadeOut(motionSpec()),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(CONTROL_INSET),
        ) {
            PlayPauseButton(
                isPlaying = playWhenReady,
                onClick = { if (player.playWhenReady) player.pause() else player.play() },
            )
        }
        Box(Modifier.align(Alignment.Center).videoFrame(photo)) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(motionSpec()),
                exit = fadeOut(motionSpec()),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(BADGE_INSET),
            ) {
                PlaybackTime(player, photo.durationMs ?: 0L)
            }
        }
    }
}

/** 영상이 실제로 보이는 자리. 카드가 영상보다 높을 때 길이·재생 시간 표시가 검은 띠에 걸치지 않게 한다. */
private fun Modifier.videoFrame(photo: AlbumPhoto): Modifier {
    val width = photo.width ?: 0
    val height = photo.height ?: 0
    return if (width > 0 && height > 0) aspectRatio(width.toFloat() / height) else fillMaxSize()
}

/** 지금 위치와 전체 길이. 길이를 아직 모르면 앨범에 저장한 길이를 쓴다. */
@OptIn(UnstableApi::class)
@Composable
private fun PlaybackTime(player: Player, fallbackDurationMs: Long) {
    val progress = rememberProgressStateWithTickInterval(player, PROGRESS_TICK_MS)
    val duration = progress.durationMs.takeIf { it > 0 } ?: fallbackDurationMs
    Text(
        stringResource(R.string.album_video_progress, formatClock(progress.currentPositionMs), formatClock(duration)),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .background(GLASS, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun VideoPoster(photo: AlbumPhoto) {
    val image by rememberFileImage(photo.thumbnailPath)
    image?.let {
        Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

/**
 * 카드 오른쪽 아래의 둥근 버튼. 가로 동영상은 카드가 낮아 가운데에 두면 날짜·메모와 겹치므로 늘 이 자리에 둔다.
 * 흐린 검정 바탕에 흰 아이콘이라 밝은 장면 위에서도 보인다.
 */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(if (isPlaying) R.string.album_video_pause else R.string.album_video_play)
    Box(
        modifier
            .size(PLAY_BUTTON)
            .clip(CircleShape)
            .background(GLASS, CircleShape)
            .border(1.dp, Color.White.copy(alpha = EDGE_ALPHA), CircleShape)
            .pressable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(PLAY_ICON),
        )
    }
}

/** 앨범 동영상 하나만 재생하는 플레이어. 다른 앱 소리와 번갈아 나도록 오디오 포커스를 따르고, 이어폰을 빼면 멈춘다. */
private fun albumPlayer(context: Context, path: String, startAtMs: Long): ExoPlayer = ExoPlayer.Builder(context)
    .setAudioAttributes(
        AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
        true,
    )
    .setHandleAudioBecomingNoisy(true)
    .build()
    .apply {
        setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))), startAtMs)
        prepare()
        playWhenReady = true
    }

private val PLAY_ICON = 32.dp
private val CONTROL_INSET = 12.dp
private val BADGE_INSET = 12.dp
private const val EDGE_ALPHA = 0.2f
private const val PROGRESS_TICK_MS = 500L

/** 재생 버튼 지름. 날짜·메모 줄이 이만큼 비켜 준다. */
internal val PLAY_BUTTON = 56.dp

/** 카드 오른쪽에서 재생 버튼과 그 여백이 차지하는 폭. */
internal val VIDEO_CONTROL_SPACE = PLAY_BUTTON + 12.dp * 2
