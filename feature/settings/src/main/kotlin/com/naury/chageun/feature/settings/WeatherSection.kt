package com.naury.chageun.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionCheck
import com.naury.chageun.core.domain.weather.KoreaRegionListInfo
import com.naury.chageun.core.domain.weather.KoreaRegionRepository
import com.naury.chageun.core.domain.weather.KoreaRegionUpdates
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.WeatherLocationPicker
import com.naury.chageun.core.ui.weatherLocationSettingLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DateTimeException
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WeatherSettingsViewModel @Inject constructor(
    private val repository: WeatherSettingsRepository,
    private val regions: KoreaRegionRepository,
    private val updates: KoreaRegionUpdates,
) : ViewModel() {

    val settings: StateFlow<WeatherSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WeatherSettings())

    /** "현재 위치"로 마지막에 찾은 동네 이름. 홈에서 날씨를 받을 때 채워진다. */
    val areaName: StateFlow<String?> = repository.currentAreaName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** 지난번에 "현재 위치"를 구하지 못했다. */
    val isLocationMissing: StateFlow<Boolean> = repository.isCurrentLocationMissing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    /** 지역 고르기 창 맨 위에 보여 줄 최근에 고른 지역. */
    val recentRegionCodes: StateFlow<List<String>> = repository.recentRegionCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    /** 지금 쓰는 지역 목록의 판과 출처. 설정 › 정보 출처와 "지역 목록 업데이트 확인"에 보인다. */
    val regionListInfo: StateFlow<KoreaRegionListInfo?> = regions.listInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val _regionListCheck = MutableStateFlow<RegionListCheckState>(RegionListCheckState.Idle)

    /** "지역 목록 업데이트 확인"을 누른 결과. */
    val regionListCheck: StateFlow<RegionListCheckState> = _regionListCheck.asStateFlow()

    /** 지역 고르기 창을 열 때 읽는 전국 지역 목록. 목록을 확인한 지 일주일이 지났으면 새 판도 확인한다. */
    suspend fun regionCatalog(): KoreaRegionCatalog {
        viewModelScope.launch { updates.checkIfStale() }
        return regions.catalog()
    }

    fun checkRegionList() {
        if (_regionListCheck.value == RegionListCheckState.Checking) return
        _regionListCheck.value = RegionListCheckState.Checking
        viewModelScope.launch { _regionListCheck.value = RegionListCheckState.Done(updates.checkNow()) }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(enabled) }
    }

    fun setLocation(location: WeatherLocation) {
        viewModelScope.launch { repository.setLocation(location) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

@Composable
internal fun WeatherSectionRoute(viewModel: WeatherSettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val areaName by viewModel.areaName.collectAsStateWithLifecycle()
    val isLocationMissing by viewModel.isLocationMissing.collectAsStateWithLifecycle()
    val recentRegionCodes by viewModel.recentRegionCodes.collectAsStateWithLifecycle()
    val regionListInfo by viewModel.regionListInfo.collectAsStateWithLifecycle()
    val regionListCheck by viewModel.regionListCheck.collectAsStateWithLifecycle()
    WeatherSection(
        settings,
        onEnabledChange = viewModel::setEnabled,
        onLocationPicked = viewModel::setLocation,
        loadRegions = viewModel::regionCatalog,
        areaName = areaName,
        isLocationMissing = isLocationMissing,
        recentRegionCodes = recentRegionCodes,
        regionList = RegionListRowState(regionListInfo, regionListCheck, viewModel::checkRegionList),
    )
}

/** 설정 › 날씨의 "지역 목록 업데이트 확인"을 누른 결과. */
sealed interface RegionListCheckState {
    data object Idle : RegionListCheckState

    data object Checking : RegionListCheckState

    data class Done(val result: KoreaRegionCheck) : RegionListCheckState
}

/** "지역 목록 업데이트 확인" 줄. [info]가 null이면 아직 판을 읽는 중이다. */
data class RegionListRowState(
    val info: KoreaRegionListInfo? = null,
    val check: RegionListCheckState = RegionListCheckState.Idle,
    val onCheck: () -> Unit = {},
)

/**
 * 홈 날씨를 켜고 끄고, 날씨를 볼 위치를 바꾼다. 위치 권한은 "현재 위치"를 고를 때만 묻는다.
 *
 * 직접 고른 지역은 "서울 강남구 역삼1동"처럼 시·도부터 전체 이름을 보여 준다.
 *
 * @param loadRegions 지역 고르기 창을 열 때 읽는 전국 지역 목록.
 * @param areaName "현재 위치"로 찾은 동네 이름. 있으면 "현재 위치 (서울 강남구)"처럼 함께 보여 준다.
 * @param isLocationMissing 지난번에 기기 위치를 구하지 못했다. "현재 위치 (찾지 못함)"으로 보여 준다.
 * @param recentRegionCodes 최근에 고른 지역. 지역 고르기 창 맨 위에 보여 준다.
 * @param regionList 지역 목록 판과 업데이트 확인. null이면 그 줄을 보이지 않는다.
 */
@Composable
fun WeatherSection(
    settings: WeatherSettings,
    onEnabledChange: (Boolean) -> Unit,
    onLocationPicked: (WeatherLocation) -> Unit,
    loadRegions: suspend () -> KoreaRegionCatalog,
    areaName: String? = null,
    isLocationMissing: Boolean = false,
    recentRegionCodes: List<String> = emptyList(),
    regionList: RegionListRowState? = null,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    CardGroup(stringResource(R.string.settings_section_weather)) {
        ToggleListRow(
            icon = Icons.Filled.WbSunny,
            title = stringResource(R.string.settings_weather_enabled),
            body = stringResource(R.string.settings_weather_enabled_body),
            checked = settings.isEnabled,
            onCheckedChange = onEnabledChange,
            tone = ChageunTheme.colors.upcoming,
        )
        GroupDivider()
        ListRow(
            icon = Icons.Filled.Place,
            title = stringResource(R.string.settings_weather_location),
            body = settings.location?.let { weatherLocationSettingLabel(it, areaName, isLocationMissing) }
                ?: stringResource(R.string.settings_weather_location_none),
            onClick = { picking = true },
            enabled = settings.isEnabled,
        )
        regionList?.let {
            GroupDivider()
            ListRow(
                icon = Icons.Filled.Sync,
                title = stringResource(R.string.settings_region_list_check),
                body = regionListCheckBody(it),
                onClick = it.onCheck,
                enabled = it.check != RegionListCheckState.Checking,
            )
        }
    }
    if (picking) {
        WeatherLocationPicker(
            onPicked = { location ->
                picking = false
                onLocationPicked(location)
            },
            onDismiss = { picking = false },
            loadRegions = loadRegions,
            recentRegionCodes = recentRegionCodes,
        )
    }
}

@Composable
private fun regionListCheckBody(state: RegionListRowState): String? {
    val month = state.info?.let { regionListMonth(it.version) }
    return when (val check = state.check) {
        RegionListCheckState.Checking -> stringResource(R.string.settings_region_list_checking)
        RegionListCheckState.Idle -> month?.let { stringResource(R.string.settings_region_list_current, it) }
        is RegionListCheckState.Done -> when (val result = check.result) {
            KoreaRegionCheck.UpToDate -> month?.let { stringResource(R.string.settings_region_list_up_to_date, it) }
            is KoreaRegionCheck.Updated ->
                stringResource(R.string.settings_region_list_updated, regionListMonth(result.version))
            is KoreaRegionCheck.Failed -> stringResource(R.string.settings_region_list_failed)
        }
    }
}

/** "2026-10" → "2026년 10월", "October 2026". 형식이 다르면 그대로 쓴다. */
@Composable
internal fun regionListMonth(version: String): String {
    val pattern = stringResource(R.string.settings_region_list_month_pattern)
    val locale = LocalConfiguration.current.locales[0]
    return remember(version, pattern, locale) {
        try {
            YearMonth.parse(version).format(DateTimeFormatter.ofPattern(pattern, locale))
        } catch (_: DateTimeException) {
            version
        }
    }
}
