package com.naury.chageun.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLocale
import java.text.NumberFormat
import java.time.LocalDate
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
