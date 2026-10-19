package com.naury.chageun.feature.home

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.weather.WeatherTip
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.ui.weatherLocationLabel
import com.naury.kinetica.ui.KIconButton
import com.naury.kinetica.ui.KVariant
import kotlin.math.roundToInt

/**
 * 인사말·날짜 아래 날씨 한 줄. 날씨 자체는 Hero 바탕 하늘로 보여 주고, 여기에는 글로 짧게 적는다.
 * 날씨를 껐거나 받지 못했으면 아무것도 그리지 않는다.
 *
 * @param seed 팁 문구를 고르는 값. 인사말과 같이 날마다 바뀌고 하루 동안은 그대로다.
 * @param onChooseLocation 위치를 고르는 창을 연다. 날씨 줄을 누르거나 켜기 안내를 누를 때 부른다.
 */
@Composable
internal fun HomeWeatherLine(
    weather: HomeWeather,
    seed: Long,
    onChooseLocation: () -> Unit,
    onDismissPrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (weather) {
        HomeWeather.Off, HomeWeather.Unavailable -> Unit
        HomeWeather.Setup -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            WeatherPill(Icons.Filled.WbSunny, stringResource(R.string.home_weather_enable), onChooseLocation)
            // 아이콘은 버튼 지름의 절반으로 그려진다. 작은 버튼을 두되 누르는 영역은 48dp를 지킨다.
            KIconButton(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.home_weather_dismiss),
                onClick = onDismissPrompt,
                modifier = Modifier.minimumInteractiveComponentSize(),
                variant = KVariant.Text,
                size = DISMISS_BUTTON_SIZE,
                contentColor = skyVariantText(),
            )
        }
        HomeWeather.NeedsPermission -> WeatherPill(
            Icons.Filled.LocationOff,
            stringResource(R.string.home_weather_permission),
            onChooseLocation,
            modifier,
        )
        HomeWeather.LocationUnavailable -> WeatherPill(
            Icons.Filled.LocationSearching,
            stringResource(R.string.home_weather_location_failed),
            onChooseLocation,
            modifier,
        )
        HomeWeather.Loading -> Text(
            stringResource(R.string.home_weather_loading),
            style = MaterialTheme.typography.bodySmall,
            color = skyVariantText(),
            modifier = modifier.padding(vertical = ChageunTheme.spacing.xs),
        )
        is HomeWeather.Shown -> WeatherNow(weather, seed, onChooseLocation, modifier)
    }
}

/**
 * "☁ 27° 흐림 · 서울 강남구   오늘도 안전 운전해요   날씨: MET Norway". 날씨·팁·출처를 덩어리로 두어,
 * 폭이 좁으면 덩어리째 다음 줄로 넘어간다. 화면 읽기는 한 문장으로 읽고 누르면 위치를 바꾼다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherNow(weather: HomeWeather.Shown, seed: Long, onChooseLocation: () -> Unit, modifier: Modifier) {
    val snapshot = weather.snapshot
    val tips = stringArrayResource(weather.tip.arrayRes)
    val tip = tips[Math.floorMod(seed, tips.size)]
    val temperature = snapshot.temperatureC.roundToInt().toString()
    val condition = stringResource(snapshot.condition.labelRes)
    val place = weatherLocationLabel(weather.location, weather.areaName)
    val spoken = stringResource(R.string.home_weather_spoken, place, temperature, condition, tip)
    val variant = skyVariantText()
    val summary = buildAnnotatedString {
        appendInlineContent(ICON_ID, "·")
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(" ")
            append(stringResource(R.string.home_weather_summary, temperature, condition))
        }
        withStyle(SpanStyle(color = variant)) { append(" · $place") }
    }
    val icon = weather.icon
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .pressable(
                onClick = onChooseLocation,
                style = PressStyle.Highlight,
                onClickLabel = stringResource(R.string.home_weather_change_location),
            )
            .clearAndSetSemantics { contentDescription = spoken }
            .heightIn(min = MIN_TOUCH)
            .padding(vertical = ChageunTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Text(
            summary,
            style = MaterialTheme.typography.bodyMedium,
            inlineContent = mapOf(
                ICON_ID to InlineTextContent(Placeholder(ICON_EM.em, ICON_EM.em, PlaceholderVerticalAlign.TextCenter)) {
                    Icon(icon, contentDescription = null)
                },
            ),
            modifier = Modifier.alignByBaseline(),
        )
        Text(tip, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.alignByBaseline())
        // CC BY 4.0 출처 표시. 작게 두되 대비는 지킨다.
        Text(
            stringResource(R.string.home_weather_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = variant,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/**
 * 새 지역 목록에서 고른 날씨 지역이 없어졌을 때 한 번 띄우는 안내. 누르면 지역을 다시 고르고, ✕로 닫는다.
 * 그동안 날씨는 위 시·군·구나 시·도로 보여 준다.
 */
@Composable
internal fun RegionChangedNotice(onChooseAgain: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        WeatherPill(
            Icons.Filled.EditLocationAlt,
            stringResource(R.string.home_weather_region_changed),
            onChooseAgain,
            Modifier.weight(1f, fill = false),
        )
        // 아이콘은 버튼 지름의 절반으로 그려진다. 작은 버튼을 두되 누르는 영역은 48dp를 지킨다.
        KIconButton(
            Icons.Filled.Close,
            contentDescription = stringResource(R.string.home_weather_region_changed_dismiss),
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize(),
            variant = KVariant.Text,
            size = DISMISS_BUTTON_SIZE,
            contentColor = skyVariantText(),
        )
    }
}

/** 하늘 위에 떠 있는 작은 알약 버튼. 보이는 크기는 작아도 누르는 영역은 48dp를 지킨다. */
@Composable
private fun WeatherPill(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .pressable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = PILL_ALPHA))
            .padding(horizontal = ChageunTheme.spacing.sm, vertical = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/**
 * 하늘 위 보조 글자색. 화면 기본 보조색보다 한 단계 진하게(다크에서는 밝게) 두어,
 * 비·뇌우처럼 어두운 하늘에서도 대비 4.5:1을 넘긴다.
 */
@Composable
internal fun skyVariantText(): Color = if (isDarkBackground()) SKY_VARIANT_TEXT_DARK else SKY_VARIANT_TEXT_LIGHT

@Composable
internal fun isDarkBackground(): Boolean = MaterialTheme.colorScheme.background.luminance() < HALF

internal val SKY_VARIANT_TEXT_LIGHT = Color(0xFF3C3C43)
internal val SKY_VARIANT_TEXT_DARK = Color(0xFFC7C7CC)
private const val HALF = 0.5f
private const val ICON_ID = "weather-icon"
private const val ICON_EM = 1.3f
private const val PILL_ALPHA = 0.75f
private val DISMISS_BUTTON_SIZE = 36.dp
private val MIN_TOUCH = 48.dp

private val HomeWeather.Shown.icon: ImageVector
    get() = when (tip) {
        WeatherTip.StrongWind -> Icons.Filled.Air
        WeatherTip.Cold, WeatherTip.Freezing, WeatherTip.Heat -> Icons.Filled.Thermostat
        else -> snapshot.condition.icon(scene == WeatherScene.ClearNight)
    }

private fun WeatherCondition.icon(isNight: Boolean): ImageVector = when (this) {
    WeatherCondition.Clear -> if (isNight) Icons.Filled.NightsStay else Icons.Filled.WbSunny
    WeatherCondition.PartlyCloudy -> Icons.Filled.FilterDrama
    WeatherCondition.Cloudy -> Icons.Filled.Cloud
    WeatherCondition.Fog -> Icons.Filled.Dehaze
    WeatherCondition.Rain, WeatherCondition.HeavyRain -> Icons.Filled.Umbrella
    WeatherCondition.Thunder -> Icons.Filled.Thunderstorm
    WeatherCondition.Sleet, WeatherCondition.Snow -> Icons.Filled.AcUnit
}

@get:StringRes
private val WeatherCondition.labelRes: Int
    get() = when (this) {
        WeatherCondition.Clear -> R.string.home_weather_condition_clear
        WeatherCondition.PartlyCloudy -> R.string.home_weather_condition_partly_cloudy
        WeatherCondition.Cloudy -> R.string.home_weather_condition_cloudy
        WeatherCondition.Fog -> R.string.home_weather_condition_fog
        WeatherCondition.Rain -> R.string.home_weather_condition_rain
        WeatherCondition.HeavyRain -> R.string.home_weather_condition_heavy_rain
        WeatherCondition.Thunder -> R.string.home_weather_condition_thunder
        WeatherCondition.Sleet -> R.string.home_weather_condition_sleet
        WeatherCondition.Snow -> R.string.home_weather_condition_snow
    }

@get:ArrayRes
private val WeatherTip.arrayRes: Int
    get() = when (this) {
        WeatherTip.Snow -> R.array.home_weather_tip_snow
        WeatherTip.Thunder -> R.array.home_weather_tip_thunder
        WeatherTip.HeavyRain -> R.array.home_weather_tip_heavy_rain
        WeatherTip.Rain -> R.array.home_weather_tip_rain
        WeatherTip.Fog -> R.array.home_weather_tip_fog
        WeatherTip.StrongWind -> R.array.home_weather_tip_strong_wind
        WeatherTip.Cold -> R.array.home_weather_tip_cold
        WeatherTip.Freezing -> R.array.home_weather_tip_freezing
        WeatherTip.Heat -> R.array.home_weather_tip_heat
        WeatherTip.NiceDay -> R.array.home_weather_tip_nice_day
        WeatherTip.Calm -> R.array.home_weather_tip_calm
    }
