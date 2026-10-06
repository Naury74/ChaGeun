package com.naury.chageun.core.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLocale
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalLocale.current.platformLocale

@Composable
@ReadOnlyComposable
fun formatNumber(value: Long): String = NumberFormat.getIntegerInstance(currentLocale()).format(value)

@Composable
@ReadOnlyComposable
fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale()))

/** 월·일만 짧게. 예: 한국어 "10. 6.", 영어 "10/6". */
@Composable
@ReadOnlyComposable
fun formatMonthDay(date: LocalDate): String {
    val locale = currentLocale()
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "Md"), locale))
}

@Composable
@ReadOnlyComposable
fun formatYearMonth(month: YearMonth): String {
    val locale = currentLocale()
    return month.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "yMMMM"), locale))
}

/** 밀리리터를 소수점 둘째 자리까지의 리터로 표시한다. 예: 41176 → "41.18". */
@Composable
@ReadOnlyComposable
fun formatLitres(volumeMl: Long): String = NumberFormat.getNumberInstance(currentLocale()).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(volumeMl / ML_PER_LITRE)

private const val ML_PER_LITRE = 1_000.0
