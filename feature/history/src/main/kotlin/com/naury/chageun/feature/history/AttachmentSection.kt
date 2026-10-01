package com.naury.chageun.feature.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.ui.rememberFileImage

@Composable
internal fun AttachmentSection(
    attachments: List<Attachment>,
    failedCount: Int,
    onAttach: (List<String>) -> Unit,
    onDelete: (String) -> Unit,
    onDismissFailure: () -> Unit,
) {
    var viewingId by rememberSaveable { mutableStateOf<String?>(null) }
    val remaining = MAX_ATTACHMENTS_PER_RECORD - attachments.size
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_ATTACHMENTS_PER_RECORD),
    ) { uris -> onAttach(uris.take(remaining).map { it.toString() }) }

    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(R.string.attachment_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (attachments.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                items(attachments, key = { it.id }) { attachment ->
                    Thumbnail(attachment, onClick = { viewingId = attachment.id })
                }
            }
        }
        if (failedCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(R.plurals.attachment_failed, failedCount, failedCount),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissFailure) { Text(stringResource(R.string.attachment_close)) }
            }
        }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = remaining > 0,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.attachment_add, MAX_ATTACHMENTS_PER_RECORD)) }
        Text(
            stringResource(R.string.attachment_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    attachments.firstOrNull { it.id == viewingId }?.let { attachment ->
        AttachmentViewer(
            attachment = attachment,
            onDelete = {
                viewingId = null
                onDelete(attachment.id)
            },
            onDismiss = { viewingId = null },
        )
    }
}

@Composable
private fun Thumbnail(attachment: Attachment, onClick: () -> Unit) {
    val image by rememberFileImage(attachment.thumbnailPath)
    Box(
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Image, onClickLabel = stringResource(R.string.attachment_open), onClick = onClick),
    ) {
        image?.let {
            Image(
                it,
                contentDescription = stringResource(R.string.attachment_thumbnail),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun AttachmentViewer(attachment: Attachment, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val image by rememberFileImage(attachment.filePath)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(ChageunTheme.spacing.sm)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(VIEWER_ASPECT_RATIO),
                    contentAlignment = Alignment.Center,
                ) {
                    image?.let {
                        Image(
                            it,
                            contentDescription = stringResource(R.string.attachment_thumbnail),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.history_delete)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.attachment_close)) }
                }
            }
        }
    }
}

private val THUMBNAIL_SIZE = 88.dp
private const val VIEWER_ASPECT_RATIO = 3f / 4f
