package com.naury.chageun.feature.vehicle.album

import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.naury.chageun.core.ui.OwnScreenLaunch
import com.naury.chageun.core.ui.UndoBar
import com.naury.chageun.feature.vehicle.R
import java.util.concurrent.atomic.AtomicInteger

/** 사진을 지운 뒤 잠깐 뜨는 되돌리기 막대. 시간이 다 되면 정말 지운다. */
@Composable
internal fun AlbumUndoBar(undo: AlbumUndo?, actions: AlbumActions, modifier: Modifier = Modifier) {
    UndoBar(
        message = undo?.let { undoMessage(it) },
        actionLabel = stringResource(R.string.album_undo),
        onAction = actions.onUndoDelete,
        onTimeout = actions.onFinishDelete,
        modifier = modifier,
        key = undo,
        icon = Icons.Filled.DeleteOutline,
    )
}

/** 사진만 지웠으면 "사진 n장", 동영상만이면 "동영상 n개", 섞였으면 "사진·동영상 n개"로 센다. */
@Composable
private fun undoMessage(undo: AlbumUndo): String {
    val count = undo.ids.size
    val res = when {
        undo.videoCount == 0 -> R.plurals.album_deleted_photos
        undo.photoCount == 0 -> R.plurals.album_deleted_videos
        else -> R.plurals.album_deleted_media
    }
    return pluralStringResource(res, count, count)
}

/**
 * 앨범을 떠나거나 앱이 뒤로 가면 되돌리기를 기다리던 사진을 마저 지운다. 접거나 펴서 화면을 다시 만들 때는
 * 같은 화면으로 돌아오므로 그대로 둔다. 앨범이 띄운 편집기·사진 고르기·공유 창으로 가느라 멈출 때도 돌아올 것이라
 * 그대로 두고, 그동안 안내 시간이 다 되면 막대가 지운다. 그 사이 앱이 끝나면 다음 실행 때 지운다.
 */
@Composable
internal fun FinishDeleteWhenLeaving(onLeave: () -> Unit) {
    val activity = LocalActivity.current
    val currentOnLeave by rememberUpdatedState(onLeave)
    val leave = { if (activity?.isChangingConfigurations != true) currentOnLeave() }
    val launchesAtStart = remember { AtomicInteger(OwnScreenLaunch.count) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { launchesAtStart.set(OwnScreenLaunch.count) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (OwnScreenLaunch.count == launchesAtStart.get()) leave() }
    DisposableEffect(Unit) { onDispose { leave() } }
}
