package com.naury.chageun.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.NumericTextStyles

/**
 * 차량을 보여 주는 열린 형태의 Hero 영역이다. 일부러 카드로 감싸지 않는다. 이미지는 중립적인 Hero 배경
 * 위에 놓이고 상태 영역은 그 아래에서 시작한다.
 */
@Composable
fun VehicleHeroSection(
    title: String,
    subtitle: String,
    mileage: String?,
    freshness: String?,
    modifier: Modifier = Modifier,
    photoPath: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChageunTheme.colors.heroBackground)
            .padding(ChageunTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
    ) {
        val photo = photoPath?.let { rememberFileImage(it).value }
        if (photo != null) {
            // 사용자 사진은 잘리지 않도록 고정 비율 안에 Fit으로 맞춘다 (기획서 13.4).
            Image(
                bitmap = photo,
                // 실루엣과 달리 사용자가 찍은 이 차의 사진이므로 TalkBack에 알린다.
                contentDescription = stringResource(R.string.vehicle_hero_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ChageunTheme.spacing.sm)
                    .aspectRatio(PHOTO_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.large),
            )
        } else {
            SilhouetteImage()
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            mileage?.let { Text(it, style = NumericTextStyles.Hero) }
            freshness?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        action?.invoke()
    }
}

@Composable
private fun FloorShadow(modifier: Modifier) {
    val shadow = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Canvas(modifier) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(shadow, Color.Transparent),
                center = center,
                radius = size.width / 2,
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
        )
    }
}

@Composable
private fun SilhouetteImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ChageunTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        FloorShadow(
            Modifier
                .fillMaxWidth(HERO_IMAGE_WIDTH_FRACTION)
                .height(18.dp)
                .align(Alignment.BottomCenter),
        )
        Image(
            painter = painterResource(R.drawable.vehicle_silhouette_suv),
            // 이 차의 사진이 아닌 일반 실루엣이다. 차 이름은 아래 제목에 이미 나온다.
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(HERO_IMAGE_WIDTH_FRACTION)
                .aspectRatio(HERO_IMAGE_ASPECT_RATIO),
        )
    }
}

private const val PHOTO_ASPECT_RATIO = 16f / 9f
private const val HERO_IMAGE_WIDTH_FRACTION = 0.7f
private const val HERO_IMAGE_ASPECT_RATIO = 360f / 160f

@Preview(widthDp = 360)
@Composable
private fun VehicleHeroSectionPreview() {
    ChageunTheme {
        VehicleHeroSection(
            title = "KG Mobility Torres",
            subtitle = "2023 · Gasoline",
            mileage = "42,180 km",
            freshness = "As of today",
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun VehicleHeroSectionDarkPreview() {
    ChageunTheme(darkTheme = true) {
        VehicleHeroSection(
            title = "KG Mobility Torres",
            subtitle = "2023 · Gasoline",
            mileage = "42,180 km",
            freshness = "As of today",
        )
    }
}
