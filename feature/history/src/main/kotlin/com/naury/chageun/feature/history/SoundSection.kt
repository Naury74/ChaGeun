package com.naury.chageun.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.history.MAX_SOUNDS_PER_RECORD
import com.naury.chageun.core.domain.history.MAX_SOUND_DURATION_MS
import com.naury.chageun.core.domain.history.MAX_SOUND_LABEL_LENGTH
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.ChageunOptionDialog
import com.naury.chageun.core.ui.ChageunTextField
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.chageun.core.ui.DialogOption
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.formatDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * 기록에 붙인 소리를 다루는 동작. 녹음 편집기는 화면 배치와 상관없는 자리에서 열어야 하므로 밖에서 받는다.
 *
 * @property onRecord 녹음 화면을 열어 새로 녹음한다. null이면 소리 칸을 두지 않는다(붙인 소리가 있으면 듣기만 한다).
 * @property onPickFile 다른 앱에서 녹음해 둔 소리 파일을 골라 넣는다.
 * @property onEdit 붙인 소리를 녹음 편집기로 다시 열어 그 자리에 바꿔 넣는다.
 */
data class SoundAttachmentEdits(
    val onRecord: ((RecordRef) -> Unit)? = null,
    val onPickFile: ((RecordRef) -> Unit)? = null,
    val onEdit: ((Attachment) -> Unit)? = null,
    val onRename: (String, String) -> Unit = { _, _ -> },
    val onDelete: (String) -> Unit = {},
    val onDismissNotice: () -> Unit = {},
)

/** 상세 화면의 소리 칸에 필요한 값. [items]는 붙인 순서다. */
internal data class SoundsState(
    val items: List<Attachment>,
    val notice: SoundNotice?,
    val edits: SoundAttachmentEdits,
) {
    /** 녹음할 수 있거나 이미 붙인 소리가 있을 때만 칸을 보인다. */
    fun isShownFor(ref: RecordRef): Boolean =
        ref.type in SOUND_OWNER_TYPES && (edits.onRecord != null || items.isNotEmpty())
}

/** 정비·점검 기록만 소리를 붙인다. 주유·주행거리 기록에는 들려줄 증상이 없다. */
internal val SOUND_OWNER_TYPES = setOf(
    TimelineEventType.Maintenance,
    TimelineEventType.Inspection,
    TimelineEventType.Repair,
    TimelineEventType.Note,
)

@Composable
internal fun SoundSection(state: SoundsState, owner: RecordRef, player: SoundPlayerState) {
    var menuId by rememberSaveable { mutableStateOf<String?>(null) }
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val remaining = MAX_SOUNDS_PER_RECORD - state.items.size
    LaunchedEffect(state.items) { player.sync(state.items) }

    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(R.string.sound_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (state.items.isNotEmpty()) {
            CardGroup(null) {
                state.items.forEachIndexed { index, sound ->
                    if (index > 0) GroupDivider()
                    SoundRow(sound, soundName(sound, state.items), player, onMore = { menuId = sound.id })
                }
            }
        }
        state.notice?.let { SoundNoticeRow(it, state.edits.onDismissNotice) }
        state.edits.onRecord?.let { record ->
            OutlinedButton(
                onClick = { record(owner) },
                enabled = remaining > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.sound_record))
            }
        }
        state.edits.onPickFile?.let { pick ->
            TextButton(onClick = { pick(owner) }, enabled = remaining > 0, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Outlined.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.sound_add_file))
            }
        }
        Text(
            stringResource(R.string.sound_notice, MAX_SOUNDS_PER_RECORD, MAX_SOUND_DURATION_MS / MILLIS_PER_MINUTE),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SoundDialogs(
        sounds = state.items,
        edits = state.edits,
        menuId = menuId,
        renamingId = renamingId,
        deletingId = deletingId,
        onMenu = { menuId = it },
        onRename = { renamingId = it },
        onDelete = { deletingId = it },
    )
}

@Composable
private fun SoundRow(sound: Attachment, name: String, player: SoundPlayerState, onMore: () -> Unit) {
    val isCurrent = player.currentId == sound.id
    val isPlaying = player.isPlaying(sound)
    val durationMs = sound.durationMs ?: 0L
    var positionMs by remember(sound.id) { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = player.positionMs
            delay(PROGRESS_TICK_MS)
        }
    }
    val action = stringResource(if (isPlaying) R.string.sound_pause else R.string.sound_play)
    val description = stringResource(R.string.sound_play_description, name, spokenDuration(durationMs), action)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            start = ChageunTheme.spacing.sm,
            end = ChageunTheme.spacing.xxs,
            top = ChageunTheme.spacing.xs,
            bottom = ChageunTheme.spacing.xs,
        ),
    ) {
        FilledTonalIconButton(
            onClick = { player.toggle(sound) },
            modifier = Modifier
                .size(PLAY_BUTTON_SIZE)
                .semantics { contentDescription = description },
        ) {
            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = ChageunTheme.spacing.sm),
        ) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            SoundMeta(sound, durationMs, if (isCurrent) positionMs else null)
            if (isCurrent && durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ChageunTheme.spacing.xxs),
                )
            }
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.sound_more, name))
        }
    }
}

/** 길이와 녹음한 날. 재생 중이면 지금 위치도 함께 보인다. 글자가 커지면 다음 줄로 넘어간다. */
@Composable
private fun SoundMeta(sound: Attachment, durationMs: Long, positionMs: Long?) {
    val duration = formatSoundClock(durationMs)
    val time = positionMs?.let { stringResource(R.string.sound_progress, formatSoundClock(it), duration) } ?: duration
    val date = formatDate(sound.createdAt.atZone(ZoneId.systemDefault()).toLocalDate())
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(META_ICON_SIZE),
        )
        Text(
            stringResource(R.string.sound_meta, time, date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = ChageunTheme.spacing.xxs),
        )
    }
}

@Composable
private fun SoundNoticeRow(notice: SoundNotice, onDismiss: () -> Unit) {
    val message = when (notice) {
        SoundNotice.TooLong -> stringResource(R.string.sound_error_too_long, MAX_SOUND_DURATION_MS / MILLIS_PER_MINUTE)
        SoundNotice.LimitReached -> stringResource(R.string.sound_error_limit, MAX_SOUNDS_PER_RECORD)
        SoundNotice.Failed -> stringResource(R.string.sound_error_failed)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.attachment_close)) }
    }
}

/** 더 보기 → 이름 붙이기·다시 편집·삭제. 열려 있는 창은 화면이 다시 만들어져도 id로 이어진다. */
@Composable
private fun SoundDialogs(
    sounds: List<Attachment>,
    edits: SoundAttachmentEdits,
    menuId: String?,
    renamingId: String?,
    deletingId: String?,
    onMenu: (String?) -> Unit,
    onRename: (String?) -> Unit,
    onDelete: (String?) -> Unit,
) {
    sounds.firstOrNull { it.id == menuId }?.let { sound ->
        SoundMenu(sound, soundName(sound, sounds), edits, onDismiss = { onMenu(null) }, onRename = {
            onMenu(null)
            onRename(sound.id)
        }, onDelete = {
            onMenu(null)
            onDelete(sound.id)
        })
    }
    sounds.firstOrNull { it.id == renamingId }?.let { sound ->
        RenameSoundDialog(
            initial = sound.label.orEmpty(),
            onSave = { label ->
                onRename(null)
                edits.onRename(sound.id, label)
            },
            onDismiss = { onRename(null) },
        )
    }
    sounds.firstOrNull { it.id == deletingId }?.let { sound ->
        ChageunDialog(
            title = stringResource(R.string.sound_delete_title),
            message = stringResource(R.string.sound_delete_body),
            icon = Icons.Filled.DeleteOutline,
            tone = ChageunTheme.colors.critical,
            onDismiss = { onDelete(null) },
            actions = listOf(
                DialogAction(stringResource(R.string.history_delete), DialogActionStyle.Destructive) {
                    onDelete(null)
                    edits.onDelete(sound.id)
                },
                DialogAction(stringResource(R.string.history_cancel), DialogActionStyle.Secondary) { onDelete(null) },
            ),
        )
    }
}

@Composable
private fun SoundMenu(
    sound: Attachment,
    name: String,
    edits: SoundAttachmentEdits,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val options = listOfNotNull(
        DialogOption(
            stringResource(R.string.sound_rename),
            onRename,
            Icons.Outlined.Edit,
        ),
        edits.onEdit?.let { edit ->
            DialogOption(stringResource(R.string.sound_edit_again), {
                onDismiss()
                edit(sound)
            }, Icons.Outlined.ContentCut)
        },
        DialogOption(
            stringResource(R.string.history_delete),
            onDelete,
            Icons.Filled.DeleteOutline,
            ChageunTheme.colors.critical,
        ),
    )
    ChageunOptionDialog(
        title = name,
        options = options,
        cancelLabel = stringResource(R.string.history_cancel),
        onDismiss = onDismiss,
    )
}

@Composable
private fun RenameSoundDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    ChageunDialog(
        title = stringResource(R.string.sound_rename_title),
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(R.string.sound_save)) { onSave(text) },
            DialogAction(stringResource(R.string.history_cancel), DialogActionStyle.Secondary, onClick = onDismiss),
        ),
    ) {
        ChageunTextField(
            value = text,
            onValueChange = { text = it.replace('\n', ' ').take(MAX_SOUND_LABEL_LENGTH) },
            label = stringResource(R.string.sound_rename_label),
            placeholder = stringResource(R.string.sound_rename_placeholder),
            supportingText = stringResource(R.string.sound_label_counter, text.length, MAX_SOUND_LABEL_LENGTH),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 붙인 이름, 없으면 "소리 2"처럼 붙인 순서. */
@Composable
private fun soundName(sound: Attachment, sounds: List<Attachment>): String = sound.label?.takeIf { it.isNotBlank() }
    ?: stringResource(R.string.sound_default_name, sounds.indexOfFirst { it.id == sound.id } + 1)

/** TalkBack이 읽을 길이. 예: "12초", "1분 5초". */
@Composable
private fun spokenDuration(durationMs: Long): String {
    val totalSeconds = (durationMs + HALF_SECOND_MS) / MILLIS_PER_SECOND
    val minutes = (totalSeconds / SECONDS_PER_MINUTE).toInt()
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toInt()
    return if (minutes == 0) {
        pluralStringResource(R.plurals.sound_duration_seconds, seconds, seconds)
    } else {
        stringResource(R.string.sound_duration_minutes_seconds, minutes, seconds)
    }
}

/** 화면에 보일 길이. 예: "0:12", "2:05". */
internal fun formatSoundClock(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0) + HALF_SECOND_MS) / MILLIS_PER_SECOND
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / SECONDS_PER_MINUTE, totalSeconds % SECONDS_PER_MINUTE)
}

private val PLAY_BUTTON_SIZE = 48.dp
private val META_ICON_SIZE = 14.dp
private const val PROGRESS_TICK_MS = 200L
private const val MILLIS_PER_SECOND = 1_000L
private const val HALF_SECOND_MS = 500L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_MINUTE = 60_000L
