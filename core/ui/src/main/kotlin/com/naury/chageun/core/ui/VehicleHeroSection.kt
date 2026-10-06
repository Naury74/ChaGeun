package com.naury.chageun.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.motionSpec
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
    bodyType: VehicleBodyType = VehicleBodyType.Sedan,
    photoStatus: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChageunTheme.colors.heroBackground)
            .padding(ChageunTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
    ) {
        HeroImage(photoPath, bodyType)
        photoStatus?.invoke()
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

/** Hero 사진 자리에 그릴 것. 사진이 바뀔 때 무엇에서 무엇으로 바뀌는지 알아야 자연스럽게 넘길 수 있다. */
private sealed interface HeroVisual {
    data class Photo(val image: ImageBitmap, val isCutout: Boolean) : HeroVisual

    /** 사진을 읽는 중. 실루엣을 그리면 탭을 오갈 때 실루엣과 사진이 번갈아 깜빡이므로 빈 자리만 둔다. */
    data class Placeholder(val isCutout: Boolean) : HeroVisual

    data object Silhouette : HeroVisual
}

@Composable
private fun HeroImage(photoPath: String?, bodyType: VehicleBodyType) {
    val isCutout = photoPath?.endsWith(".png", ignoreCase = true) == true
    val state = photoPath?.let { rememberFileImageState(it).value } ?: FileImageState.Missing
    // 배경을 지운 사진이 생겨 경로가 바뀌면 새 파일을 읽는 동안 이전 사진을 그대로 두었다가 겹쳐 바꾼다.
    var lastPhoto by remember { mutableStateOf<HeroVisual.Photo?>(null) }
    val visual = when (state) {
        is FileImageState.Loaded -> HeroVisual.Photo(state.image, isCutout)
        FileImageState.Loading -> lastPhoto ?: HeroVisual.Placeholder(isCutout)
        FileImageState.Missing -> HeroVisual.Silhouette
    }
    val fade = motionSpec<Float>()
    LaunchedEffect(visual) {
        lastPhoto =
            visual as? HeroVisual.Photo ?: lastPhoto.takeIf { visual !is HeroVisual.Silhouette }
    }
    AnimatedContent(
        targetState = visual,
        transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) using SizeTransform(clip = false) },
        contentAlignment = Alignment.Center,
        label = "hero-image",
    ) { shown ->
        when (shown) {
            // 차만 잘라 낸 사진은 투명 PNG로 저장된다. 실루엣처럼 바닥 그림자 위에 배경 없이 놓는다.
            is HeroVisual.Photo -> if (shown.isCutout) {
                GroundedImage {
                    Image(
                        bitmap = shown.image,
                        contentDescription = stringResource(R.string.vehicle_hero_photo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.cutoutSize(),
                    )
                }
            } else {
                // 사용자 사진은 잘리지 않도록 고정 비율 안에 Fit으로 맞춘다 (기획서 13.4).
                Image(
                    bitmap = shown.image,
                    // 실루엣과 달리 사용자가 찍은 이 차의 사진이므로 TalkBack에 알린다.
                    contentDescription = stringResource(R.string.vehicle_hero_photo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(vertical = ChageunTheme.spacing.sm)
                        .photoSize()
                        .clip(MaterialTheme.shapes.large),
                )
            }
            // 같은 크기의 빈 자리를 두어 사진이 나타날 때 아래 내용이 밀리지 않게 한다.
            is HeroVisual.Placeholder -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = ChageunTheme.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Box(if (shown.isCutout) Modifier.cutoutSize() else Modifier.photoSize())
            }
            HeroVisual.Silhouette -> SilhouetteImage(bodyType)
        }
    }
}

private fun Modifier.cutoutSize() = fillMaxWidth(CUTOUT_WIDTH_FRACTION).aspectRatio(CUTOUT_ASPECT_RATIO)

private fun Modifier.photoSize() = fillMaxWidth().aspectRatio(PHOTO_ASPECT_RATIO)

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
private fun SilhouetteImage(bodyType: VehicleBodyType) {
    GroundedImage {
        Image(
            painter = painterResource(bodyType.silhouetteRes),
            // 이 차의 사진이 아닌 일반 실루엣이다. 차 이름은 아래 제목에 이미 나온다.
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(HERO_IMAGE_WIDTH_FRACTION)
                .aspectRatio(HERO_IMAGE_ASPECT_RATIO),
        )
    }
}

/** 차 그림 아래에 바닥 그림자를 깔아 떠 있지 않고 서 있는 것처럼 보이게 한다. */
@Composable
private fun GroundedImage(image: @Composable () -> Unit) {
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
        image()
    }
}

private const val PHOTO_ASPECT_RATIO = 16f / 9f
private const val CUTOUT_WIDTH_FRACTION = 0.8f
private const val CUTOUT_ASPECT_RATIO = 2f
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
