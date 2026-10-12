package com.naury.chageun.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.naury.chageun.core.designsystem.motion.rememberReduceMotion
import com.naury.chageun.core.domain.weather.WeatherTips
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.model.WeatherSnapshot
import com.naury.chageun.core.ui.HeroSky
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** 홈 Hero 바탕에 그리는 날씨 장면. 같은 장면은 같은 하늘색과 움직임을 쓴다. */
enum class WeatherScene {
    ClearDay,
    ClearNight,
    PartlyCloudy,
    Cloudy,
    Wind,
    Fog,
    Rain,
    HeavyRain,
    Thunder,
    Snow,
    Heat,
    Cold,
}

/**
 * 날씨로 장면을 고른다. 위에서부터 먼저 맞는 것을 쓰고, 남은 맑음·구름 조금은 밤낮으로 나눈다.
 * 밤은 제공처가 밤 아이콘을 줬거나 현지 시각이 18시부터 6시 전까지일 때다.
 */
fun weatherSceneFor(snapshot: WeatherSnapshot, hour: Int): WeatherScene {
    val matched = SCENE_RULES.firstOrNull { (matches, _) -> matches(snapshot) }?.second
    val isNight = snapshot.isNight || hour < DAY_START_HOUR || hour >= NIGHT_START_HOUR
    return matched ?: when {
        isNight -> WeatherScene.ClearNight
        snapshot.condition == WeatherCondition.PartlyCloudy -> WeatherScene.PartlyCloudy
        else -> WeatherScene.ClearDay
    }
}

private const val DAY_START_HOUR = 6
private const val NIGHT_START_HOUR = 18

private val SCENE_RULES: List<Pair<(WeatherSnapshot) -> Boolean, WeatherScene>> = listOf(
    { s: WeatherSnapshot -> s.condition == WeatherCondition.Snow || s.condition == WeatherCondition.Sleet } to
        WeatherScene.Snow,
    { s: WeatherSnapshot -> s.condition == WeatherCondition.Thunder } to WeatherScene.Thunder,
    { s: WeatherSnapshot -> s.condition == WeatherCondition.HeavyRain } to WeatherScene.HeavyRain,
    { s: WeatherSnapshot -> s.condition == WeatherCondition.Rain } to WeatherScene.Rain,
    { s: WeatherSnapshot -> s.condition == WeatherCondition.Fog } to WeatherScene.Fog,
    { s: WeatherSnapshot -> s.windSpeedMs >= WeatherTips.STRONG_WIND_MS } to WeatherScene.Wind,
    { s: WeatherSnapshot -> s.temperatureC >= WeatherTips.HEAT_C } to WeatherScene.Heat,
    { s: WeatherSnapshot -> s.temperatureC <= WeatherTips.COLD_C } to WeatherScene.Cold,
    { s: WeatherSnapshot -> s.condition == WeatherCondition.Cloudy } to WeatherScene.Cloudy,
)

/**
 * 장면의 색. [sky]는 Hero 하늘색과 햇빛 번짐, [ink]는 빗줄기·눈송이·별 색, [cloud]는 구름·안개 색이다.
 * 하늘 위 글자 대비(4.5:1 이상)는 WeatherSkyTest가 장면마다 확인한다.
 */
data class SkyPalette(val sky: HeroSky, val ink: Color = Color.Transparent, val cloud: Color = Color.Transparent)

fun skyPalette(scene: WeatherScene, isDark: Boolean): SkyPalette =
    (if (isDark) DarkSkies else LightSkies).getValue(scene)

// 라이트 모드는 글자가 어두우므로 밤도 옅은 남보라 하늘로 둔다. 깊은 남색 밤하늘은 다크 모드에서 쓴다.
private val LightSkies: Map<WeatherScene, SkyPalette> = mapOf(
    WeatherScene.ClearDay to SkyPalette(HeroSky(Color(0xFFA6CFFF), Color(0xCCFFF3D1))),
    WeatherScene.ClearNight to SkyPalette(HeroSky(Color(0xFFB4BCDF), Color(0x33FFFFFF)), ink = Color(0xE6FFFFFF)),
    WeatherScene.PartlyCloudy to
        SkyPalette(HeroSky(Color(0xFFB3CCEB), Color(0x99FFFFFF)), cloud = Color(0x8CFFFFFF)),
    WeatherScene.Cloudy to SkyPalette(HeroSky(Color(0xFFBCC7D6), Color(0x66FFFFFF)), cloud = Color(0x99FFFFFF)),
    WeatherScene.Wind to SkyPalette(HeroSky(Color(0xFFB8C6D8), Color(0x66FFFFFF)), cloud = Color(0x99FFFFFF)),
    WeatherScene.Fog to SkyPalette(HeroSky(Color(0xFFC7CDD5), Color(0x4DFFFFFF)), cloud = Color(0xB3F4F5F7)),
    WeatherScene.Rain to SkyPalette(
        HeroSky(Color(0xFFAEBBCD), Color(0x33FFFFFF)),
        ink = Color(0x4D4A5A72),
        cloud = Color(0x59FFFFFF),
    ),
    WeatherScene.HeavyRain to SkyPalette(
        HeroSky(Color(0xFFA7B4C7), Color(0x1AFFFFFF)),
        ink = Color(0x5C4A5A72),
        cloud = Color(0x4DFFFFFF),
    ),
    WeatherScene.Thunder to SkyPalette(
        HeroSky(Color(0xFFA5AEBF), Color(0x1AFFFFFF)),
        ink = Color(0x5C4A5A72),
        cloud = Color(0x405A6478),
    ),
    WeatherScene.Snow to SkyPalette(HeroSky(Color(0xFFC3D3E6), Color(0x66FFFFFF)), ink = Color(0xFFFFFFFF)),
    WeatherScene.Heat to SkyPalette(HeroSky(Color(0xFFFFD9B3), Color(0xB3FFE0B0))),
    WeatherScene.Cold to SkyPalette(HeroSky(Color(0xFFB5D3F2), Color(0x80FFFFFF))),
)

private val DarkSkies: Map<WeatherScene, SkyPalette> = mapOf(
    WeatherScene.ClearDay to SkyPalette(HeroSky(Color(0xFF15335F), Color(0x1AFFE7B0))),
    WeatherScene.ClearNight to SkyPalette(HeroSky(Color(0xFF070F26), Color(0x14FFFFFF)), ink = Color(0xB3FFFFFF)),
    WeatherScene.PartlyCloudy to
        SkyPalette(HeroSky(Color(0xFF15253F), Color(0x14FFFFFF)), cloud = Color(0x14FFFFFF)),
    WeatherScene.Cloudy to SkyPalette(HeroSky(Color(0xFF1A2230), Color(0x12FFFFFF)), cloud = Color(0x14FFFFFF)),
    WeatherScene.Wind to SkyPalette(HeroSky(Color(0xFF18212F), Color(0x12FFFFFF)), cloud = Color(0x14FFFFFF)),
    WeatherScene.Fog to SkyPalette(HeroSky(Color(0xFF1D232B), Color(0x14FFFFFF)), cloud = Color(0x1AFFFFFF)),
    WeatherScene.Rain to SkyPalette(
        HeroSky(Color(0xFF121C2A), Color(0x0DFFFFFF)),
        ink = Color(0x47A9BCD8),
        cloud = Color(0x0FFFFFFF),
    ),
    WeatherScene.HeavyRain to SkyPalette(
        HeroSky(Color(0xFF0F1824), Color(0x0AFFFFFF)),
        ink = Color(0x57A9BCD8),
        cloud = Color(0x0FFFFFFF),
    ),
    WeatherScene.Thunder to SkyPalette(
        HeroSky(Color(0xFF0D131D), Color(0x08FFFFFF)),
        ink = Color(0x57A9BCD8),
        cloud = Color(0x0FFFFFFF),
    ),
    WeatherScene.Snow to SkyPalette(HeroSky(Color(0xFF17233A), Color(0x14FFFFFF)), ink = Color(0xA6FFFFFF)),
    WeatherScene.Heat to SkyPalette(HeroSky(Color(0xFF33200F), Color(0x26FF9A4D))),
    WeatherScene.Cold to SkyPalette(HeroSky(Color(0xFF0C2342), Color(0x1A9ECBFF))),
)

/** 테스트에서 하늘 애니메이션을 이 시각(ms)의 한 장면으로 멈춘다. null이면 실제 프레임을 따른다. */
internal val LocalFrozenSkyTime = staticCompositionLocalOf<Long?> { null }

/**
 * 하늘 위, 차와 글자 아래에 그리는 날씨 장식. 순수 장식이라 접근성 트리에서 숨긴다.
 *
 * 움직임은 화면이 보일 때(STARTED)만 초당 약 25번 갱신하고, '애니메이션 삭제'가 켜져 있으면 멈춘 한 장면으로 둔다.
 * 하늘이 끝나는 높이(Hero의 [SKY_FRACTION])로 갈수록, 옆 칸이 있으면 오른쪽 끝으로 갈수록 흐려진다.
 */
@Composable
internal fun WeatherSkyOverlay(scene: WeatherScene, palette: SkyPalette, fadesAtEnd: Boolean) {
    val particles = remember(scene) { SkyParticles.of(scene) }
    val time = rememberSkyTime(animated = particles.isAnimated)
    Canvas(
        Modifier
            .fillMaxSize()
            .semantics { hideFromAccessibility() }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val seconds = time.value / MILLIS_PER_SECOND
        val skyHeight = size.height * SKY_FRACTION
        particles.draw(this, palette, seconds, skyHeight)
        fadeOut(skyHeight, fadesAtEnd)
    }
}

/** 하늘 아래쪽과 옆 칸 쪽 가장자리를 투명하게 지워 바탕으로 자연스럽게 넘어가게 한다. */
private fun DrawScope.fadeOut(skyHeight: Float, fadesAtEnd: Boolean) {
    drawRect(
        Brush.verticalGradient(
            0f to Color.Black,
            skyHeight * FADE_START to Color.Black,
            skyHeight to Color.Transparent,
        ),
        blendMode = BlendMode.DstIn,
    )
    if (!fadesAtEnd) return
    val brush = if (layoutDirection == LayoutDirection.Rtl) {
        Brush.horizontalGradient(0f to Color.Transparent, 1f - END_FADE to Color.Black)
    } else {
        Brush.horizontalGradient(END_FADE to Color.Black, 1f to Color.Transparent)
    }
    drawRect(brush, blendMode = BlendMode.DstIn)
}

@Composable
private fun rememberSkyTime(animated: Boolean): State<Long> {
    val frozen = LocalFrozenSkyTime.current
    val reduceMotion = rememberReduceMotion()
    val time = remember { mutableLongStateOf(frozen ?: STILL_FRAME_MS) }
    if (frozen != null || !animated || reduceMotion) return time
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // 다시 보일 때 처음부터 시작하지 않도록 멈춘 시각에서 이어 간다.
            val origin = withFrameMillis { it } - time.longValue
            while (isActive) {
                withFrameMillis { time.longValue = it - origin }
                delay(FRAME_INTERVAL_MS)
            }
        }
    }
    return time
}

/** 장면별 입자. 위치는 고정 시드로 정해 같은 시각이면 언제나 같은 그림이 나온다. */
private class SkyParticles(
    scene: WeatherScene,
    private val rain: List<Drop>,
    private val snow: List<Flake>,
    private val clouds: List<Cloud>,
    private val stars: List<Star>,
) {
    private val heavy = scene != WeatherScene.Rain
    private val fog = scene == WeatherScene.Fog
    private val cloudSpeedDp = if (scene == WeatherScene.Wind) WIND_CLOUD_SPEED_DP else CLOUD_SPEED_DP
    val isAnimated: Boolean = rain.isNotEmpty() || snow.isNotEmpty() || clouds.isNotEmpty() || fog

    fun draw(scope: DrawScope, palette: SkyPalette, seconds: Float, skyHeight: Float) = with(scope) {
        if (fog) drawFog(palette.cloud, seconds, skyHeight)
        clouds.forEach { drawCloud(it, palette.cloud, seconds, skyHeight, cloudSpeedDp.dp.toPx()) }
        stars.forEach { drawStar(it, palette.ink, skyHeight) }
        rain.forEach { drawDrop(it, palette.ink, seconds, skyHeight, heavy) }
        snow.forEach { drawFlake(it, palette.ink, seconds, skyHeight) }
    }

    companion object {
        fun of(scene: WeatherScene): SkyParticles {
            val random = Random(scene.ordinal + SEED)
            val rainCount = when (scene) {
                WeatherScene.Rain -> RAIN_COUNT
                WeatherScene.HeavyRain, WeatherScene.Thunder -> HEAVY_RAIN_COUNT
                else -> 0
            }
            val cloudCount = when (scene) {
                WeatherScene.PartlyCloudy -> 2
                WeatherScene.Cloudy, WeatherScene.Wind, WeatherScene.Rain, WeatherScene.HeavyRain,
                WeatherScene.Thunder,
                -> CLOUD_COUNT
                else -> 0
            }
            return SkyParticles(
                scene = scene,
                rain = List(rainCount) { newDrop(random) },
                snow = if (scene == WeatherScene.Snow) List(SNOW_COUNT) { newFlake(random) } else emptyList(),
                clouds = List(cloudCount) { newCloud(random, it) },
                stars = if (scene == WeatherScene.ClearNight) List(STAR_COUNT) { newStar(random) } else emptyList(),
            )
        }
    }
}

private class Drop(val x: Float, val y: Float, val speed: Float, val alpha: Float)

private class Flake(
    val x: Float,
    val y: Float,
    val speed: Float,
    val radiusDp: Float,
    val phase: Float,
    val alpha: Float,
)

private class Cloud(val x: Float, val y: Float, val width: Float)

private class Star(val x: Float, val y: Float, val radiusDp: Float, val alpha: Float)

// 입자마다 위치·속도·밝기를 조금씩 다르게 뽑는다. 범위는 화면에서 보며 은은하게 맞췄다.
private val newDrop: (Random) -> Drop = { r ->
    Drop(r.nextFloat(), r.nextFloat(), speed = 0.9f + r.nextFloat() * 0.6f, alpha = 0.45f + r.nextFloat() * 0.55f)
}

private val newFlake: (Random) -> Flake = { r ->
    Flake(
        x = r.nextFloat(),
        y = r.nextFloat(),
        speed = 0.05f + r.nextFloat() * 0.06f,
        radiusDp = 1.2f + r.nextFloat() * 1.6f,
        phase = r.nextFloat() * TWO_PI,
        alpha = 0.55f + r.nextFloat() * 0.45f,
    )
}

// 구름은 서로 겹치지 않게 가로로 고르게 흩어 놓는다.
private val newCloud: (Random, Int) -> Cloud = { r, index ->
    Cloud(
        x = index * 0.42f + r.nextFloat() * 0.2f,
        y = 0.06f + r.nextFloat() * 0.22f,
        width =
        0.5f + r.nextFloat() * 0.25f,
    )
}

private val newStar: (Random) -> Star = { r ->
    Star(
        r.nextFloat(),
        y = 0.03f + r.nextFloat() * 0.4f,
        radiusDp = 0.7f + r.nextFloat() * 0.9f,
        alpha =
        0.3f + r.nextFloat() * 0.7f,
    )
}

/** 오른쪽 위에서 왼쪽 아래로 비스듬히 떨어지는 가는 빗줄기. */
private fun DrawScope.drawDrop(drop: Drop, ink: Color, seconds: Float, skyHeight: Float, heavy: Boolean) {
    val length = (if (heavy) HEAVY_DROP_LENGTH_DP else DROP_LENGTH_DP).dp.toPx()
    val travel = skyHeight + length
    val fallSpeed = skyHeight * drop.speed * (if (heavy) HEAVY_SPEED_FACTOR else 1f)
    val y = (drop.y * travel + seconds * fallSpeed) % travel - length
    val x = ((drop.x * size.width - y * RAIN_SLANT) % size.width + size.width) % size.width
    drawLine(
        color = ink.copy(alpha = ink.alpha * drop.alpha),
        start = Offset(x + length * RAIN_SLANT, y),
        end = Offset(x, y + length),
        strokeWidth = DROP_WIDTH_DP.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

/** 좌우로 천천히 흔들리며 내리는 눈송이. */
private fun DrawScope.drawFlake(flake: Flake, ink: Color, seconds: Float, skyHeight: Float) {
    val radius = flake.radiusDp.dp.toPx()
    val travel = skyHeight + radius * 2
    val y = (flake.y * travel + seconds * skyHeight * flake.speed) % travel - radius
    val sway = sin(seconds * SNOW_SWAY_SPEED + flake.phase) * SNOW_SWAY_DP.dp.toPx()
    drawCircle(ink.copy(alpha = ink.alpha * flake.alpha), radius, Offset(flake.x * size.width + sway, y))
}

/** 겹친 원 세 개를 흐리게 그린 구름 덩어리. 오른쪽으로 천천히 흘러가고 화면 밖으로 나가면 왼쪽에서 다시 나온다. */
private fun DrawScope.drawCloud(cloud: Cloud, color: Color, seconds: Float, skyHeight: Float, speedPx: Float) {
    val width = cloud.width * size.width
    val span = size.width + width
    val left = (cloud.x * span + seconds * speedPx) % span - width
    val centerY = cloud.y * skyHeight
    CLOUD_PUFFS.forEach { (dx, dy, scale) ->
        val center = Offset(left + width * dx, centerY + width * dy)
        val radius = width * scale
        drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
    }
}

private fun DrawScope.drawStar(star: Star, ink: Color, skyHeight: Float) {
    drawCircle(
        ink.copy(alpha = ink.alpha * star.alpha),
        star.radiusDp.dp.toPx(),
        Offset(
            star.x * size.width,
            star.y * skyHeight,
        ),
    )
}

/** 아래로 갈수록 짙어지는 안개 층과, 그 위를 천천히 오가는 옅은 안개 띠. */
private fun DrawScope.drawFog(color: Color, seconds: Float, skyHeight: Float) {
    drawRect(
        Brush.verticalGradient(
            0f to Color.Transparent,
            skyHeight * FOG_LAYER_START to Color.Transparent,
            skyHeight * FOG_LAYER_PEAK to color,
            skyHeight to color,
        ),
        size = Size(size.width, skyHeight),
    )
    FOG_BANDS.forEachIndexed { index, (y, phase) ->
        val drift = sin(seconds * FOG_DRIFT_SPEED + phase) * size.width * FOG_DRIFT
        val center = Offset(size.width * (FOG_BAND_START + index * FOG_BAND_GAP) + drift, skyHeight * y)
        val radius = size.width * FOG_BAND_RADIUS
        drawOval(
            Brush.radialGradient(listOf(color, Color.Transparent), center, radius),
            topLeft = Offset(center.x - radius, center.y - radius * FOG_BAND_FLATNESS),
            size = Size(radius * 2, radius * 2 * FOG_BAND_FLATNESS),
        )
    }
}

/** 기본 하늘과 같은 높이에서 하늘이 끝난다(VehicleHeroSection의 SKY_END). */
private const val SKY_FRACTION = 0.62f
private const val FADE_START = 0.55f
private const val END_FADE = 0.7f
private const val MILLIS_PER_SECOND = 1000f
private const val FRAME_INTERVAL_MS = 40L
private const val STILL_FRAME_MS = 2_400L
private const val SEED = 2_026
private const val TWO_PI = 6.2831855f

private const val RAIN_COUNT = 42
private const val HEAVY_RAIN_COUNT = 60
private const val SNOW_COUNT = 36
private const val STAR_COUNT = 14
private const val CLOUD_COUNT = 3
private const val CLOUD_SPEED_DP = 5f
private const val WIND_CLOUD_SPEED_DP = 34f
private const val DROP_LENGTH_DP = 12f
private const val HEAVY_DROP_LENGTH_DP = 16f
private const val DROP_WIDTH_DP = 1.1f
private const val RAIN_SLANT = 0.26f
private const val HEAVY_SPEED_FACTOR = 1.25f
private const val SNOW_SWAY_SPEED = 0.7f
private const val SNOW_SWAY_DP = 9f
private const val FOG_LAYER_START = 0.2f
private const val FOG_LAYER_PEAK = 0.75f
private const val FOG_DRIFT_SPEED = 0.12f
private const val FOG_DRIFT = 0.08f
private const val FOG_BAND_RADIUS = 0.55f
private const val FOG_BAND_START = 0.3f
private const val FOG_BAND_GAP = 0.4f
private const val FOG_BAND_FLATNESS = 0.22f

// 구름 한 덩어리를 이루는 원: 가로 위치, 세로 위치, 반지름(모두 구름 폭 기준 비율)
private val CLOUD_PUFFS = listOf(
    Triple(0.3f, 0.04f, 0.26f),
    Triple(0.55f, -0.02f, 0.32f),
    Triple(0.78f, 0.05f, 0.24f),
)

// 안개 띠: 세로 위치(하늘 높이 비율), 흔들림 위상
private val FOG_BANDS = listOf(0.5f to 0f, 0.72f to 2.1f)
