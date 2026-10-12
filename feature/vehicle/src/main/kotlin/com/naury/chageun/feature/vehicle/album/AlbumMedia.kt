package com.naury.chageun.feature.vehicle.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.feature.vehicle.R
import java.util.Locale

/**
 * 화면 읽기 프로그램이 읽을 이름. 사진은 메모나 찍은 날이고, 동영상은 앞에 "동영상, 42초"를 붙여 사진과 구분한다.
 */
@Composable
internal fun mediaDescription(photo: AlbumPhoto): String {
    if (!photo.isVideo) return photo.comment ?: stringResource(R.string.album_photo_on, formatDate(photo.takenOn))
    return stringResource(
        R.string.album_video_description,
        spokenDuration(photo.durationMs ?: 0L),
        photo.comment ?: formatDate(photo.takenOn),
    )
}

/** "1분 5초"처럼 소리 내어 읽기 좋은 길이. 1초보다 짧으면 1초로 읽는다. */
@Composable
internal fun spokenDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / MILLIS_PER_SECOND).coerceAtLeast(1)
    val minutes = (totalSeconds / SECONDS_PER_MINUTE).toInt()
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toInt()
    val minutePart = pluralStringResource(R.plurals.album_duration_minutes, minutes, minutes)
    val secondPart = pluralStringResource(R.plurals.album_duration_seconds, seconds, seconds)
    return when {
        minutes == 0 -> secondPart
        seconds == 0 -> minutePart
        else -> stringResource(R.string.album_duration_minutes_seconds, minutePart, secondPart)
    }
}

/** 동영상 칸·카드에 쓰는 "0:42" 같은 짧은 시간. 숫자와 콜론뿐이라 언어에 따라 바꾸지 않는다. */
internal fun formatClock(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0) + MILLIS_PER_SECOND / 2) / MILLIS_PER_SECOND
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / SECONDS_PER_MINUTE, totalSeconds % SECONDS_PER_MINUTE)
}

/**
 * 재생 표시와 길이를 담은 작은 알약. 그리드 칸과 사진 보기 카드에 겹쳐 동영상임을 알린다.
 * 화면 읽기 프로그램에는 칸 이름이 이미 "동영상, 42초"로 읽히므로 여기서는 따로 읽지 않는다.
 */
@Composable
internal fun VideoDurationBadge(durationMs: Long?, modifier: Modifier = Modifier, showDuration: Boolean = true) {
    Row(
        modifier
            .background(BADGE_FILL, CircleShape)
            .padding(start = 3.dp, end = if (showDuration) 7.dp else 3.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(BADGE_ICON),
        )
        if (showDuration) {
            Text(
                formatClock(durationMs ?: 0L),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private val BADGE_FILL = Color.Black.copy(alpha = 0.55f)
private val BADGE_ICON = 14.dp
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
