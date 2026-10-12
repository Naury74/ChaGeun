package com.naury.chageun.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSettings
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.WeatherLocationPicker
import com.naury.chageun.core.ui.weatherLocationLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WeatherSettingsViewModel @Inject constructor(private val repository: WeatherSettingsRepository) : ViewModel() {

    val settings: StateFlow<WeatherSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WeatherSettings())

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
    WeatherSection(settings, onEnabledChange = viewModel::setEnabled, onLocationPicked = viewModel::setLocation)
}

/** 홈 날씨를 켜고 끄고, 날씨를 볼 위치를 바꾼다. 위치 권한은 "현재 위치"를 고를 때만 묻는다. */
@Composable
fun WeatherSection(
    settings: WeatherSettings,
    onEnabledChange: (Boolean) -> Unit,
    onLocationPicked: (WeatherLocation) -> Unit,
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
            body = settings.location?.let { weatherLocationLabel(it) }
                ?: stringResource(R.string.settings_weather_location_none),
            onClick = { picking = true },
            enabled = settings.isEnabled,
        )
    }
    if (picking) {
        WeatherLocationPicker(
            onPicked = { location ->
                picking = false
                onLocationPicked(location)
            },
            onDismiss = { picking = false },
        )
    }
}
