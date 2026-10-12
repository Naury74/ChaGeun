package com.naury.chageun.feature.home

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import com.naury.chageun.core.domain.weather.WeatherTip
import com.naury.chageun.core.model.WeatherCondition
import com.naury.chageun.core.ui.ItemIconBadge
import com.naury.chageun.core.ui.weatherLocationLabel
import kotlin.math.roundToInt

/**
 * 인사말 아래의 날씨 카드. 날씨를 껐거나 받지 못했으면 아무것도 그리지 않는다.
 * 하늘 바탕이 비치도록 반투명한 표면 위에 둔다.
 *
 * @param seed 팁 문구를 고르는 값. 인사말과 같이 날마다 바뀌고 하루 동안은 그대로다.
 */
@Composable
internal fun HomeWeatherCard(weather: HomeWeather, seed: Long, onSetUp: () -> Unit, modifier: Modifier = Modifier) {
    when (weather) {
        HomeWeather.Off, HomeWeather.Unavailable -> Unit
        HomeWeather.Setup -> WeatherPrompt(
            icon = Icons.Filled.WbSunny,
            title = stringResource(R.string.home_weather_setup_title),
            body = stringResource(R.string.home_weather_setup_body),
            action = stringResource(R.string.home_weather_setup_action),
            onClick = onSetUp,
            modifier = modifier,
        )
        HomeWeather.NeedsPermission -> WeatherPrompt(
            icon = Icons.Filled.LocationOff,
            title = stringResource(R.string.home_weather_permission_title),
            body = stringResource(R.string.home_weather_permission_body),
            action = stringResource(R.string.home_weather_permission_action),
            onClick = onSetUp,
            modifier = modifier,
        )
        HomeWeather.Loading -> WeatherSurface(modifier) {
            Row(
                Modifier.padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(R.string.home_weather_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is HomeWeather.Shown -> WeatherNow(weather, seed, modifier)
    }
}

@Composable
private fun WeatherNow(weather: HomeWeather.Shown, seed: Long, modifier: Modifier) {
    val snapshot = weather.snapshot
    val tips = stringArrayResource(weather.tip.arrayRes)
    val variant = Math.floorMod(seed, tips.size)
    val summary = stringResource(
        R.string.home_weather_summary,
        snapshot.temperatureC.roundToInt().toString(),
        stringResource(snapshot.condition.labelRes),
    )
    val place = weatherLocationLabel(weather.location)
    val variantColor = MaterialTheme.colorScheme.onSurfaceVariant
    WeatherSurface(modifier.semantics(mergeDescendants = true) {}) {
        Row(
            Modifier.padding(ChageunTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            ItemIconBadge(weather.icon, weather.tip.tone, size = ICON_SIZE)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(summary) }
                        withStyle(SpanStyle(color = variantColor)) { append(" · $place") }
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(tips[variant], style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.home_weather_attribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = variantColor,
                )
            }
        }
    }
}

/** 아직 날씨를 볼 수 없을 때 위치를 고르게 하는 카드. 카드 전체가 하나의 버튼이다. */
@Composable
private fun WeatherPrompt(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    WeatherSurface(modifier.pressable(onClick = onClick)) {
        Row(
            Modifier.padding(ChageunTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            ItemIconBadge(
                icon,
                ToneColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
                size = ICON_SIZE,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    action,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = ChageunTheme.spacing.xxs),
                )
            }
        }
    }
}

@Composable
private fun WeatherSurface(modifier: Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = SURFACE_ALPHA),
        content = content,
    )
}

private val ICON_SIZE = 40.dp
private const val SURFACE_ALPHA = 0.8f

private val HomeWeather.Shown.icon: ImageVector
    get() = when (tip) {
        WeatherTip.StrongWind -> Icons.Filled.Air
        WeatherTip.Cold, WeatherTip.Freezing, WeatherTip.Heat -> Icons.Filled.Thermostat
        else -> snapshot.condition.icon(snapshot.isNight)
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

private val WeatherTip.tone: ToneColors
    @Composable get() = when {
        isCaution -> ChageunTheme.colors.upcoming
        this == WeatherTip.NiceDay -> ChageunTheme.colors.good
        else -> ToneColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
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
