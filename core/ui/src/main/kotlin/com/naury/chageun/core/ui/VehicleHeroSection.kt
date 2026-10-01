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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.NumericTextStyles

/**
 * Open hero for the vehicle. Deliberately not a card: the image sits on the neutral hero background
 * and status surfaces start below it.
 */
@Composable
fun VehicleHeroSection(
    title: String,
    subtitle: String,
    mileage: String?,
    freshness: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChageunTheme.colors.heroBackground)
            .padding(ChageunTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
    ) {
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
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(HERO_IMAGE_WIDTH_FRACTION)
                    .aspectRatio(HERO_IMAGE_ASPECT_RATIO),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        mileage?.let { Text(it, style = NumericTextStyles.Hero) }
        freshness?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
