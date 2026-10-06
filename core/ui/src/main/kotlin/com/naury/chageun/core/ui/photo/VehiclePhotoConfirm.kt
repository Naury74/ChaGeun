package com.naury.chageun.core.ui.photo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.R
import com.naury.chageun.core.ui.ToggleListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 고른 내 차 사진을 대표 사진으로 넣기 전에 배경을 지울지 고른다. 기본은 이전에 고른 방식이고,
 * 넣은 뒤에도 내 차 탭에서 언제든 바꿀 수 있다.
 */
@Composable
fun VehiclePhotoConfirmSheet(
    sourceUri: String,
    initialRemoveBackground: Boolean,
    isExpanded: Boolean,
    onApply: (removeBackground: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var removeBackground by rememberSaveable(sourceUri) { mutableStateOf(initialRemoveBackground) }
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(ChageunTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
        ) {
            Text(
                stringResource(R.string.vehicle_photo_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            val preview = rememberPreview(sourceUri)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(PREVIEW_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                preview?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.vehicle_hero_photo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            CardGroup(null) {
                ToggleListRow(
                    icon = Icons.Outlined.AutoFixHigh,
                    title = stringResource(R.string.vehicle_photo_remove_background),
                    body = stringResource(R.string.vehicle_photo_remove_background_body),
                    checked = removeBackground,
                    onCheckedChange = { removeBackground = it },
                    tone = ChageunTheme.colors.ai,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.vehicle_photo_confirm_cancel))
                }
                Button(
                    onClick = { onApply(removeBackground) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.vehicle_photo_confirm_apply)) }
            }
        }
    }
}

@Composable
private fun rememberPreview(uri: String): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching { PhotoFiles.decodeUpright(context, uri, PREVIEW_MAX_EDGE)?.asImageBitmap() }.getOrNull()
        }
    }
    return image
}

private const val PREVIEW_ASPECT_RATIO = 16f / 10f
private const val PREVIEW_MAX_EDGE = 1_280
