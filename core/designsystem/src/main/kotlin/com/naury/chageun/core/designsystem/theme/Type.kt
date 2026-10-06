package com.naury.chageun.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

private const val TABULAR_FIGURES = "tnum"

// 한국어는 띄어쓰기 단위로 줄을 바꿔야 자연스럽다. 기본값은 글자 단위라 '채 / 울 수'처럼 단어 중간에서 끊긴다.
private val KoreanLineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)
private val KoreanHeadingBreak = LineBreak.Heading.copy(wordBreak = LineBreak.WordBreak.Phrase)

internal val ChageunTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = TABULAR_FIGURES,
            lineBreak = KoreanHeadingBreak,
        ),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold, lineBreak = KoreanHeadingBreak),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, lineBreak = KoreanHeadingBreak),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold, lineBreak = KoreanHeadingBreak),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, lineBreak = KoreanHeadingBreak),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold, lineBreak = KoreanHeadingBreak),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold, lineBreak = KoreanHeadingBreak),
        bodyLarge = bodyLarge.copy(lineBreak = KoreanLineBreak),
        bodyMedium = bodyMedium.copy(lineBreak = KoreanLineBreak),
        bodySmall = bodySmall.copy(lineBreak = KoreanLineBreak),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, lineBreak = KoreanHeadingBreak),
        labelMedium = labelMedium.copy(lineBreak = KoreanHeadingBreak),
        labelSmall = labelSmall.copy(lineBreak = KoreanHeadingBreak),
    )
}

/** 숫자 스타일은 숫자 폭을 고정한다. 주행거리와 금액이 정렬되고 애니메이션 중에 흔들리지 않게 하기 위해서다. */
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
