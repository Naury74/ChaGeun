package com.naury.chageun.core.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.KoreaRegion
import com.naury.chageun.core.model.WeatherLocation

/**
 * 홈 날씨를 볼 위치를 고른다. 먼저 "현재 위치"와 "지역 직접 고르기" 중 하나를 묻는다.
 *
 * "현재 위치"를 고르면 그때 처음으로 대략적인 위치 권한(ACCESS_COARSE_LOCATION)을 묻는다. 거절하면 시·도 목록으로
 * 넘어가 권한 없이도 날씨를 볼 수 있게 한다.
 *
 * @param startWithRegions true면 첫 질문 없이 시·도 목록부터 연다.
 */
@Composable
fun WeatherLocationPicker(
    onPicked: (WeatherLocation) -> Unit,
    onDismiss: () -> Unit,
    startWithRegions: Boolean = false,
) {
    val context = LocalContext.current
    var showsRegions by rememberSaveable { mutableStateOf(startWithRegions) }
    var wasDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onPicked(WeatherLocation.Current)
        } else {
            wasDenied = true
            showsRegions = true
        }
    }
    if (showsRegions) {
        RegionDialog(
            message = if (wasDenied) stringResource(R.string.weather_location_denied_message) else null,
            onPicked = { onPicked(WeatherLocation.Region(it)) },
            onDismiss = onDismiss,
        )
    } else {
        ChageunOptionDialog(
            title = stringResource(R.string.weather_location_title),
            message = stringResource(R.string.weather_location_message),
            options = listOf(
                DialogOption(
                    label = stringResource(R.string.weather_location_current),
                    icon = Icons.Filled.MyLocation,
                    tone = ChageunTheme.colors.good,
                    onClick = {
                        if (context.hasCoarseLocationPermission()) {
                            onPicked(WeatherLocation.Current)
                        } else {
                            context.launchExternal {
                                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        }
                    },
                ),
                DialogOption(
                    label = stringResource(R.string.weather_location_pick_region),
                    icon = Icons.Filled.Map,
                    onClick = { showsRegions = true },
                ),
            ),
            cancelLabel = stringResource(R.string.weather_location_cancel),
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun RegionDialog(message: String?, onPicked: (KoreaRegion) -> Unit, onDismiss: () -> Unit) {
    ChageunOptionDialog(
        title = stringResource(R.string.weather_location_regions_title),
        message = message,
        options = KoreaRegion.entries.map { region ->
            // 열일곱 줄에 같은 아이콘을 반복하면 어수선해서 이름만 둔다.
            DialogOption(label = stringResource(region.labelRes), onClick = { onPicked(region) })
        },
        cancelLabel = stringResource(R.string.weather_location_cancel),
        onDismiss = onDismiss,
    )
}

/**
 * 고른 위치의 이름. "현재 위치"는 기기 위치로 찾은 동네 이름(예: "서울 강남구")을 쓰고,
 * 아직 못 찾았거나 지오코더가 없으면 "현재 위치"라고만 쓴다.
 */
@Composable
fun weatherLocationLabel(location: WeatherLocation, areaName: String? = null): String = when (location) {
    WeatherLocation.Current -> areaName ?: stringResource(R.string.weather_location_current_short)
    is WeatherLocation.Region -> stringResource(location.region.labelRes)
}

/**
 * 설정에 쓰는 위치 이름. 기기 위치로 찾은 동네임을 알 수 있게 "현재 위치 (서울 강남구)"처럼 쓰고,
 * 지난번에 위치를 못 구했으면 [isMissing]이라 "현재 위치 (찾지 못함)"으로 쓴다.
 */
@Composable
fun weatherLocationSettingLabel(
    location: WeatherLocation,
    areaName: String? = null,
    isMissing: Boolean = false,
): String = when {
    location != WeatherLocation.Current -> weatherLocationLabel(location)
    isMissing -> stringResource(R.string.weather_location_current_missing)
    areaName != null -> stringResource(R.string.weather_location_current_with_area, areaName)
    else -> weatherLocationLabel(location)
}

fun Context.hasCoarseLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

val KoreaRegion.labelRes: Int
    get() = when (this) {
        KoreaRegion.Seoul -> R.string.weather_region_seoul
        KoreaRegion.Busan -> R.string.weather_region_busan
        KoreaRegion.Daegu -> R.string.weather_region_daegu
        KoreaRegion.Incheon -> R.string.weather_region_incheon
        KoreaRegion.Gwangju -> R.string.weather_region_gwangju
        KoreaRegion.Daejeon -> R.string.weather_region_daejeon
        KoreaRegion.Ulsan -> R.string.weather_region_ulsan
        KoreaRegion.Sejong -> R.string.weather_region_sejong
        KoreaRegion.Gyeonggi -> R.string.weather_region_gyeonggi
        KoreaRegion.Gangwon -> R.string.weather_region_gangwon
        KoreaRegion.Chungbuk -> R.string.weather_region_chungbuk
        KoreaRegion.Chungnam -> R.string.weather_region_chungnam
        KoreaRegion.Jeonbuk -> R.string.weather_region_jeonbuk
        KoreaRegion.Jeonnam -> R.string.weather_region_jeonnam
        KoreaRegion.Gyeongbuk -> R.string.weather_region_gyeongbuk
        KoreaRegion.Gyeongnam -> R.string.weather_region_gyeongnam
        KoreaRegion.Jeju -> R.string.weather_region_jeju
    }
