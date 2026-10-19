package com.naury.chageun.feature.history

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.naury.chageun.core.designsystem.component.ChageunCard
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.jellyPress

/**
 * 첨부 사진을 사진 편집기로 고치는 동작. 편집기는 화면 배치와 상관없는 자리에서 열어야 하므로 밖에서 받는다.
 *
 * @property onEdit 첨부한 사진 한 장을 고쳐 그 자리에 바꿔 넣는다.
 * @property onAddEdited 사진을 고른 뒤 편집해서 첨부한다.
 * @property onScan 카메라로 영수증을 찍고 문서 보정을 거쳐 첨부한다.
 * @property onSharePdf 기록에 붙인 사진을 첨부한 순서대로 PDF 한 개로 묶어 공유 창으로 보낸다.
 */
data class AttachmentPhotoEdits(
    val onEdit: ((Attachment) -> Unit)? = null,
    val onAddEdited: ((RecordRef) -> Unit)? = null,
    val onScan: ((RecordRef) -> Unit)? = null,
    val onSharePdf: ((RecordDetail, List<Attachment>) -> Unit)? = null,
)

@Composable
internal fun AttachmentSection(
    attachments: List<Attachment>,
    failedCount: Int,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
    onDismissFailure: () -> Unit,
    onEdit: ((Attachment) -> Unit)? = null,
    onAddEdited: (() -> Unit)? = null,
    onScan: (() -> Unit)? = null,
    onSharePdf: (() -> Unit)? = null,
) {
    var viewingId by rememberSaveable { mutableStateOf<String?>(null) }
    val remaining = MAX_ATTACHMENTS_PER_RECORD - attachments.size

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
            onSharePdf?.let {
                AttachmentActionButton(Icons.Outlined.PictureAsPdf, stringResource(R.string.attachment_share_pdf), it)
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
                KButton(stringResource(R.string.attachment_close), onClick = onDismissFailure, variant = KVariant.Text)
            }
        }
        KButton(
            stringResource(R.string.attachment_add, MAX_ATTACHMENTS_PER_RECORD),
            onClick = onAdd,
            variant = KVariant.Outlined,
            enabled = remaining > 0,
            fullWidth = true,
        )
        onScan?.let {
            AttachmentActionButton(
                Icons.Outlined.DocumentScanner,
                stringResource(R.string.attachment_scan_receipt),
                it,
                enabled = remaining > 0,
            )
        }
        onAddEdited?.let {
            AttachmentActionButton(
                Icons.Outlined.Edit,
                stringResource(R.string.attachment_add_edited),
                it,
                enabled = remaining > 0,
            )
        }
        Text(
            stringResource(R.string.attachment_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    attachments.firstOrNull { it.id == viewingId }?.let { attachment ->
        AttachmentViewer(
            attachment = attachment,
            onEdit = onEdit?.let { edit ->
                {
                    viewingId = null
                    edit(attachment)
                }
            },
            onDelete = {
                viewingId = null
                onDelete(attachment.id)
            },
            onDismiss = { viewingId = null },
        )
    }
}

@Composable
private fun AttachmentActionButton(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    WrappingTextButton(label, icon, onClick, Modifier.fillMaxWidth(), enabled = enabled)
}

@Composable
private fun Thumbnail(attachment: Attachment, onClick: () -> Unit) {
    val image by rememberFileImage(attachment.thumbnailPath)
    Box(
        modifier = Modifier
            .jellyPress()
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
private fun AttachmentViewer(
    attachment: Attachment,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val image by rememberFileImage(attachment.filePath)
    Dialog(onDismissRequest = onDismiss) {
        ChageunCard(shape = MaterialTheme.shapes.large, popup = true) {
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
                    onEdit?.let {
                        KButton(stringResource(R.string.attachment_edit), onClick = it, variant = KVariant.Text)
                    }
                    KButton(
                        stringResource(R.string.history_delete),
                        onClick = onDelete,
                        variant = KVariant.Text,
                        tone = KTone.Destructive,
                    )
                    KButton(stringResource(R.string.attachment_close), onClick = onDismiss, variant = KVariant.Text)
                }
            }
        }
    }
}

private val THUMBNAIL_SIZE = 88.dp
private const val VIEWER_ASPECT_RATIO = 3f / 4f
