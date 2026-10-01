package com.naury.chageun.feature.vehicle

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.ui.VehicleHeroSection
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes

@Composable
fun VehicleRoute(
    onUpdateMileage: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: VehicleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isTwoPane = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
    )
    VehicleScreen(uiState, isTwoPane, onUpdateMileage, onOpenSettings = onOpenSettings)
}

@Composable
fun VehicleScreen(
    uiState: VehicleUiState,
    isTwoPane: Boolean,
    onUpdateMileage: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val state = uiState as? VehicleUiState.Content
    if (state == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val spacing = ChageunTheme.spacing
    val panes: List<LazyListScope.() -> Unit> = if (isTwoPane) {
        listOf({ overviewPane(state, onUpdateMileage) }, {
            recordsPane(state)
            settingsEntry(onOpenSettings)
        })
    } else {
        listOf({
            overviewPane(state, onUpdateMileage)
            recordsPane(state)
            settingsEntry(onOpenSettings)
        })
    }
    Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.paneGap)) {
        panes.forEach { pane ->
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                content = pane,
            )
        }
    }
}

private fun LazyListScope.overviewPane(state: VehicleUiState.Content, onUpdateMileage: () -> Unit) {
    item(key = "hero") { Hero(state, onUpdateMileage) }
    item(key = "info") { InfoSection(state) }
    item(key = "sources") {
        Section(R.string.vehicle_section_sources) {
            val res = if (state.vehicle.registrationMode == RegistrationMode.Manual) {
                R.string.vehicle_sources_manual
            } else {
                R.string.vehicle_sources_auto
            }
            Text(
                stringResource(res),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.recordsPane(state: VehicleUiState.Content) {
    item(key = "official") { OfficialDataSection() }
    item(key = "mileage-title") { SectionTitle(R.string.vehicle_section_mileage) }
    items(state.mileageLog, key = { it.id }) { entry -> MileageRow(entry) }
}

private fun LazyListScope.settingsEntry(onOpenSettings: () -> Unit) {
    item(key = "settings") {
        TextButton(onClick = onOpenSettings, modifier = Modifier.padding(horizontal = ChageunTheme.spacing.gutter)) {
            Text(stringResource(R.string.vehicle_open_settings))
        }
    }
}

@Composable
private fun Hero(state: VehicleUiState.Content, onUpdateMileage: () -> Unit) {
    val vehicle = state.vehicle
    val current = state.currentMileage
    VehicleHeroSection(
        title = "${vehicle.maker} ${vehicle.model}",
        subtitle = listOfNotNull(vehicle.modelYear?.toString(), vehicle.fuelType?.let { stringResource(it.labelRes) })
            .let { parts ->
                if (parts.size ==
                    2
                ) {
                    stringResource(R.string.vehicle_subtitle, parts[0], parts[1])
                } else {
                    parts.joinToString()
                }
            },
        mileage = current?.let { stringResource(R.string.vehicle_mileage, formatNumber(it.mileage.value)) },
        freshness = current?.let { stringResource(R.string.vehicle_mileage_as_of, formatDate(it.date)) }
            ?: stringResource(R.string.vehicle_mileage_none),
        action = { TextButton(onClick = onUpdateMileage) { Text(stringResource(R.string.vehicle_update_mileage)) } },
    )
}

@Composable
private fun InfoSection(state: VehicleUiState.Content) {
    val vehicle = state.vehicle
    Section(R.string.vehicle_section_info) {
        InfoRow(R.string.vehicle_plate, vehicle.plateMasked)
        InfoRow(R.string.vehicle_maker, vehicle.maker)
        InfoRow(R.string.vehicle_model, vehicle.model)
        InfoRow(R.string.vehicle_trim, vehicle.trim)
        InfoRow(R.string.vehicle_year, vehicle.modelYear?.toString())
        InfoRow(R.string.vehicle_fuel, vehicle.fuelType?.let { stringResource(it.labelRes) })
        InfoRow(R.string.vehicle_first_registration, vehicle.firstRegistrationDate?.let { formatDate(it) })
        InfoRow(
            R.string.vehicle_registration_mode,
            stringResource(
                if (vehicle.registrationMode == RegistrationMode.Manual) {
                    R.string.vehicle_registration_manual
                } else {
                    R.string.vehicle_registration_auto
                },
            ),
        )
    }
}

/** Until official data is connected, never imply "no recall"; point to the official services instead. */
@Composable
private fun OfficialDataSection() {
    val uriHandler = LocalUriHandler.current
    Section(R.string.vehicle_section_official) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Text(
                stringResource(R.string.vehicle_official_pending),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(ChageunTheme.spacing.md),
            )
        }
        TextButton(onClick = {
            uriHandler.openUri(RECALL_CENTER_URL)
        }) { Text(stringResource(R.string.vehicle_official_recall)) }
        TextButton(onClick = {
            uriHandler.openUri(INSPECTION_URL)
        }) { Text(stringResource(R.string.vehicle_official_inspection)) }
    }
}

@Composable
private fun MileageRow(entry: MileageEntry) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ChageunTheme.spacing.gutter, vertical = ChageunTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.vehicle_mileage, formatNumber(entry.mileage.value)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(entry.source.labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatDate(entry.date), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Section(@StringRes titleRes: Int, content: @Composable () -> Unit) {
    Column(
        Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun SectionTitle(@StringRes titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(horizontal = ChageunTheme.spacing.gutter)
            .semantics { heading() },
    )
}

@Composable
private fun InfoRow(@StringRes labelRes: Int, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        HorizontalDivider()
        Row(Modifier.padding(vertical = ChageunTheme.spacing.xs)) {
            Text(
                stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val MileageSource.labelRes: Int
    get() = when (this) {
        MileageSource.User -> R.string.vehicle_mileage_source_user
        MileageSource.Maintenance -> R.string.vehicle_mileage_source_maintenance
        MileageSource.Fuel -> R.string.vehicle_mileage_source_fuel
        MileageSource.Check -> R.string.vehicle_mileage_source_check
        MileageSource.Correction -> R.string.vehicle_mileage_source_correction
        MileageSource.Inspection -> R.string.vehicle_mileage_source_inspection
    }

private const val RECALL_CENTER_URL = "https://www.car.go.kr"
private const val INSPECTION_URL = "https://www.cyberts.kr"
