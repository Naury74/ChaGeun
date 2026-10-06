package com.naury.chageun.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.NumericTextStyles

/** Hero 아래 요약 카드의 한 칸. [unit]은 값보다 작게 붙는다(예: "42,800" + "km"). */
data class HeroStat(val label: String, val value: String, val unit: String? = null)

/**
 * 차량을 보여 주는 열린 형태의 Hero 영역이다. 일부러 카드로 감싸지 않는다.
 * 위에서 아래로 하늘색이 화면 배경으로 이어지는 바탕 위에 차 이름, 큰 차 사진, 요약 카드를 놓는다.
 *
 * @param header 하늘 바탕 맨 위에 함께 놓을 내용(예: 홈의 앱 이름 줄). 바탕이 끊기지 않게 Hero 안에 둔다.
 * @param stats 요약 카드에 나란히 놓을 값. 비어 있으면 카드를 그리지 않는다.
 * @param footnote 요약 카드 아래의 기준 안내(예: "2026. 10. 2. 기준").
 * @param skyFromTop 화면 맨 위에서 시작하면 true. 큰 제목 아래처럼 중간에서 시작하면 false로 두어
 *   하늘이 위에서 서서히 나타나게 한다. 그러지 않으면 제목 영역과 하늘 사이에 경계선이 보인다.
 * @param skyFadesAtEnd 옆에 다른 칸이 나란히 있으면 true. 하늘 오른쪽 끝을 화면 배경으로 흐리게 해 세로 경계선을 없앤다.
 */
@Composable
fun VehicleHeroSection(
    title: String,
    subtitle: String,
    stats: List<HeroStat>,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    photoPath: String? = null,
    bodyType: VehicleBodyType = VehicleBodyType.Sedan,
    header: (@Composable () -> Unit)? = null,
    skyFromTop: Boolean = true,
    skyFadesAtEnd: Boolean = false,
    photoStatus: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().skyBackdrop(skyFromTop, skyFadesAtEnd)) {
        header?.invoke()
        Column(
            modifier = Modifier.padding(ChageunTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HeroImage(photoPath, bodyType)
            photoStatus?.invoke()
            if (stats.isNotEmpty()) HeroStatsCard(stats, footnote)
            action?.invoke()
        }
    }
}

/** 위는 하늘색, 아래는 화면 배경. 오른쪽 위에 햇빛처럼 은은하게 밝은 부분을 둔다. */
@Composable
private fun Modifier.skyBackdrop(fromTop: Boolean, fadesAtEnd: Boolean): Modifier {
    val colors = ChageunTheme.colors
    val sky = if (fromTop) {
        Brush.verticalGradient(0f to colors.heroSky, SKY_END to colors.heroBackground)
    } else {
        Brush.verticalGradient(
            0f to colors.heroBackground,
            SKY_PEAK to colors.heroSky,
            SKY_END to colors.heroBackground,
        )
    }
    return drawBehind {
        drawRect(sky)
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.heroGlow, Color.Transparent),
                center = Offset(size.width * GLOW_X, if (fromTop) 0f else size.height * SKY_PEAK),
                radius = size.width * GLOW_RADIUS,
            ),
        )
        if (fadesAtEnd) {
            drawRect(
                if (layoutDirection == LayoutDirection.Rtl) {
                    Brush.horizontalGradient(0f to colors.heroBackground, 1f - END_FADE_START to Color.Transparent)
                } else {
                    Brush.horizontalGradient(END_FADE_START to Color.Transparent, 1f to colors.heroBackground)
                },
            )
        }
    }
}

@Composable
private fun HeroStatsCard(stats: List<HeroStat>, footnote: String?) {
    val colors = ChageunTheme.colors
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.heroCard,
        contentColor = colors.onHeroCard,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ChageunTheme.spacing.xs),
    ) {
        Column(
            Modifier.padding(vertical = ChageunTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                stats.forEachIndexed { index, stat ->
                    if (index > 0) {
                        VerticalDivider(
                            color = colors.onHeroCard.copy(alpha = DIVIDER_ALPHA),
                            modifier = Modifier.padding(vertical = ChageunTheme.spacing.xxs),
                        )
                    }
                    HeroStatCell(stat, Modifier.weight(1f))
                }
            }
            footnote?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onHeroCard.copy(alpha = SECONDARY_ALPHA),
                    modifier = Modifier.padding(top = ChageunTheme.spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun HeroStatCell(stat: HeroStat, modifier: Modifier) {
    // 큰 글꼴에서는 잘리지 않고 두 줄로 넘어가도록 줄 수를 묶지 않는다.
    Column(
        modifier = modifier
            .padding(horizontal = ChageunTheme.spacing.xs)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
    ) {
        Text(
            stat.label,
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA),
            textAlign = TextAlign.Center,
        )
        Text(
            buildAnnotatedString {
                append(stat.value)
                stat.unit?.let {
                    withStyle(SpanStyle(fontSize = MaterialTheme.typography.labelMedium.fontSize)) { append(" $it") }
                }
            },
            style = NumericTextStyles.Hero.copy(fontSize = STAT_VALUE_SIZE),
            textAlign = TextAlign.Center,
        )
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
                // 배경을 지우지 않은 원본은 네모 테두리가 도드라지지 않게 가장자리를 하늘 바탕으로 흐린다.
                Image(
                    bitmap = shown.image,
                    // 실루엣과 달리 사용자가 찍은 이 차의 사진이므로 TalkBack에 알린다.
                    contentDescription = stringResource(R.string.vehicle_hero_photo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(vertical = ChageunTheme.spacing.sm)
                        .photoSize()
                        .fadedEdges(shown.image.width.toFloat() / shown.image.height),
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

/**
 * 그림이 실제로 그려진 영역(Fit으로 맞춘 자리)의 가장자리를 투명하게 흐린다. 상자 비율과 사진 비율이 달라도
 * 사진 끝에서부터 흐려지도록 [imageAspectRatio]로 그려진 영역을 다시 계산한다.
 */
private fun Modifier.fadedEdges(imageAspectRatio: Float) = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val boxAspect = size.width / size.height
    val drawn = if (imageAspectRatio > boxAspect) {
        val height = size.width / imageAspectRatio
        Rect(0f, (size.height - height) / 2, size.width, (size.height + height) / 2)
    } else {
        val width = size.height * imageAspectRatio
        Rect((size.width - width) / 2, 0f, (size.width + width) / 2, size.height)
    }
    val horizontal = edgeFade(Offset(drawn.left, 0f), Offset(drawn.right, 0f), EDGE_FADE_X)
    val vertical = edgeFade(Offset(0f, drawn.top), Offset(0f, drawn.bottom), EDGE_FADE_Y)
    drawRect(horizontal, topLeft = drawn.topLeft, size = drawn.size, blendMode = BlendMode.DstIn)
    drawRect(vertical, topLeft = drawn.topLeft, size = drawn.size, blendMode = BlendMode.DstIn)
}

/** 양 끝에서 [fade]만큼 투명→불투명으로 바뀐다. 중간에 반투명 지점을 두어 선형보다 부드럽게 녹아든다. */
private fun edgeFade(from: Offset, to: Offset, fade: Float) = ShaderBrush(
    LinearGradientShader(
        from = from,
        to = to,
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = EASE_ALPHA),
            Color.Black,
            Color.Black,
            Color.Black.copy(alpha = EASE_ALPHA),
            Color.Transparent,
        ),
        colorStops = listOf(0f, fade * EASE_POINT, fade, 1f - fade, 1f - fade * EASE_POINT, 1f),
    ),
)

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
private const val CUTOUT_WIDTH_FRACTION = 1f
private const val CUTOUT_ASPECT_RATIO = 2f
private const val HERO_IMAGE_WIDTH_FRACTION = 0.85f
private const val HERO_IMAGE_ASPECT_RATIO = 360f / 160f

// 하늘색은 Hero 위쪽 절반에서 화면 배경으로 다 바뀐다. 아래의 요약 카드 주변은 화면 배경과 같아진다.
private const val SKY_END = 0.62f
private const val SKY_PEAK = 0.18f

// 원본 사진 가장자리를 흐리는 폭. 위아래는 도로·하늘이 넓게 찍히는 경우가 많아 조금 더 흐린다.
private const val EDGE_FADE_X = 0.2f
private const val EDGE_FADE_Y = 0.24f
private const val EASE_POINT = 0.5f
private const val EASE_ALPHA = 0.3f
private const val END_FADE_START = 0.7f
private const val GLOW_X = 0.85f
private const val GLOW_RADIUS = 0.75f
private const val DIVIDER_ALPHA = 0.18f
private const val SECONDARY_ALPHA = 0.7f
private val STAT_VALUE_SIZE = 20.sp

private val PREVIEW_STATS = listOf(
    HeroStat("Mileage", "42,180", "km"),
    HeroStat("Inspection", "D-14"),
    HeroStat("Last record", "Oct 6"),
)

@Preview(widthDp = 360)
@Composable
private fun VehicleHeroSectionPreview() {
    ChageunTheme {
        VehicleHeroSection(
            title = "KG Mobility Torres",
            subtitle = "2023 · Gasoline",
            stats = PREVIEW_STATS,
            footnote = "As of today",
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
            stats = PREVIEW_STATS,
            footnote = "As of today",
        )
    }
}
