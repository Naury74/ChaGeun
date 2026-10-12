package com.naury.chageun.feature.vehicle.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.ui.currentLocale
import com.naury.chageun.core.ui.photo.WatermarkText
import com.naury.chageun.feature.vehicle.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 워터마크 문구에 들어가는 앱 문구. 언어마다 다르므로 화면에서 만든다. */
internal class WatermarkLabels(val appName: String, val formatMileage: (Kilometers) -> String)

@Composable
internal fun rememberWatermarkLabels(): WatermarkLabels {
    val appName = stringResource(R.string.album_watermark_app)
    val mileageTemplate = stringResource(R.string.album_watermark_mileage)
    val locale = currentLocale()
    return remember(appName, mileageTemplate, locale) {
        val numbers = NumberFormat.getIntegerInstance(locale)
        WatermarkLabels(appName) { km -> String.format(locale, mileageTemplate, numbers.format(km.value)) }
    }
}

/**
 * 앨범 사진에 얹을 워터마크 추천 문구. "기아 스포티지 · 2026.10.12", "2026.10.12 · 60,000km", "차근 정비 기록"처럼
 * 차종·찍은 날·그날 주행거리만 쓴다. 사진을 다른 사람에게 보내는 길이라 번호판·차대번호·이름은 넣지 않는다.
 * 날짜는 사진에 찍히는 글자라 언어와 상관없이 짧은 숫자 꼴로 쓴다.
 */
internal fun albumWatermark(
    vehicle: Vehicle?,
    takenOn: LocalDate,
    mileageLog: List<MileageEntry>,
    labels: WatermarkLabels,
): WatermarkText {
    val day = takenOn.format(WATERMARK_DATE)
    val carName = vehicle?.let { "${it.maker} ${it.model}".trim() }?.takeIf { it.isNotEmpty() }
    val suggestions = listOfNotNull(
        carName?.let { "$it · $day" },
        mileageOn(mileageLog, takenOn)?.let { "$day · ${labels.formatMileage(it)}" },
        labels.appName,
        day,
    )
    return WatermarkText(text = suggestions.first(), suggestions = suggestions)
}

/**
 * [date]에 차가 달린 거리. 그날이나 그 전의 가장 가까운 기록을 쓰고, 그 전 기록이 없으면 가장 최근 기록을 쓴다.
 * 같은 날 기록이 여럿이면 나중에 넣은 값이다(기록은 최신순으로 온다).
 */
internal fun mileageOn(log: List<MileageEntry>, date: LocalDate): Kilometers? =
    (log.filter { !it.date.isAfter(date) }.maxByOrNull { it.date } ?: log.maxByOrNull { it.date })?.mileage

private val WATERMARK_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd")
