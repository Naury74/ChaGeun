package com.naury.chageun.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.photo.PhotoFiles
import com.naury.chageun.core.ui.photo.PhotoInput
import com.naury.chageun.core.ui.photo.rememberPhotoInputState
import com.naury.chageun.core.ui.vehicleBodyTypeOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 대표 사진을 고르는 단계(기획 O07). 고르지 않아도 다음으로 넘어갈 수 있고, 등록을 마치면 고른 사진을
 * 내 차 사진으로 가져와 배경을 지운다. 사진은 기기 밖으로 나가지 않는다.
 */
@Composable
internal fun PhotoStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_photo_title, R.string.onboarding_photo_body)
    val photoInput = rememberPhotoInputState()
    PhotoInput(photoInput, maxItems = 1, onPhotos = { uris, _ ->
        uris.firstOrNull()?.let { onAction(OnboardingAction.PhotoPicked(it)) }
    })
    val photo = uiState.photoUri?.let { rememberPreview(it) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = stringResource(R.string.onboarding_photo_preview),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PREVIEW_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.large),
            )
        } else {
            // 아직 고르지 않았거나 고른 파일을 읽을 수 없으면 차종 실루엣을 보여 준다.
            Box(Modifier.fillMaxWidth().aspectRatio(PREVIEW_ASPECT_RATIO), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(vehicleBodyTypeOf(uiState.model).silhouetteRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(SILHOUETTE_WIDTH_FRACTION),
                )
            }
        }
        if (photo != null) {
            CardGroup(null) {
                ToggleListRow(
                    icon = Icons.Outlined.AutoFixHigh,
                    title = stringResource(R.string.onboarding_photo_remove_background),
                    body = stringResource(R.string.onboarding_photo_remove_background_body),
                    checked = uiState.removePhotoBackground,
                    onCheckedChange = { onAction(OnboardingAction.PhotoBackgroundRemovalChanged(it)) },
                    tone = ChageunTheme.colors.ai,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            OutlinedButton(onClick = photoInput::open) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(ChageunTheme.spacing.xs))
                Text(
                    stringResource(
                        if (photo == null) R.string.onboarding_photo_pick else R.string.onboarding_photo_change,
                    ),
                )
            }
            if (uiState.photoUri != null) {
                TextButton(onClick = { onAction(OnboardingAction.RemovePhoto) }) {
                    Text(stringResource(R.string.onboarding_photo_remove))
                }
            }
        }
        Text(
            stringResource(R.string.onboarding_photo_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 고른 사진을 미리보기 크기로 읽는다. 읽는 중이거나 읽을 수 없으면 null이다. */
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
private const val SILHOUETTE_WIDTH_FRACTION = 0.8f
