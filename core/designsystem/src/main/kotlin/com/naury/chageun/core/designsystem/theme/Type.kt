package com.naury.chageun.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private const val TABULAR_FIGURES = "tnum"

internal val ChageunTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR_FIGURES),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Numeric styles keep digits at a fixed width so mileage and prices align and do not jitter while animating. */
object NumericTextStyles {
    val Hero = TextStyle(
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = TABULAR_FIGURES,
    )
    val Title = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = TABULAR_FIGURES,
    )
    val Body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR_FIGURES)
}
