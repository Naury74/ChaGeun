package com.naury.chageun.feature.vehicle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.HingeAwarePanes
import com.naury.chageun.core.ui.InfoListRow
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.LocalAnalyticsTracker
import com.naury.chageun.core.ui.VehicleHeroSection
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.isListDetailTwoPane
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.launchExternal
import com.naury.chageun.core.ui.openUriSafely
import com.naury.chageun.core.ui.vehicleBodyTypeOf
import java.time.LocalDate

@Composable
fun VehicleRoute(
    onUpdateMileage: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: VehicleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isTwoPane = isListDetailTwoPane()
    val inspectionTitle = stringResource(R.string.vehicle_inspection_record_title)
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.setPhoto(it.toString()) }
    }
    VehicleScreen(
        uiState,
        isTwoPane,
        onUpdateMileage,
        onOpenSettings = onOpenSettings,
        onInspectionDateSelected = viewModel::setInspectionDate,
        onInspectionCompleted = { viewModel.completeInspection(it, inspectionTitle) },
        photoActions = VehiclePhotoActions(
            onPick = {
                context.launchExternal {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            },
            onRemove = viewModel::removePhoto,
        ),
    )
}

data class VehiclePhotoActions(val onPick: () -> Unit = {}, val onRemove: () -> Unit = {})

@Composable
fun VehicleScreen(
    uiState: VehicleUiState,
    isTwoPane: Boolean,
    onUpdateMileage: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onInspectionDateSelected: (LocalDate?) -> Unit = {},
    onInspectionCompleted: (InspectionCompletion) -> Unit = {},
    photoActions: VehiclePhotoActions = VehiclePhotoActions(),
) {
    val state = uiState as? VehicleUiState.Content
    if (state == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val spacing = ChageunTheme.spacing
    val panes: List<LazyListScope.() -> Unit> = if (isTwoPane) {
        listOf({ overviewPane(state, onUpdateMileage, photoActions) }, {
            recordsPane(state, onInspectionDateSelected, onInspectionCompleted)
            settingsEntry(onOpenSettings)
        })
    } else {
        listOf({
            overviewPane(state, onUpdateMileage, photoActions)
            recordsPane(state, onInspectionDateSelected, onInspectionCompleted)
            settingsEntry(onOpenSettings)
        })
    }
    HingeAwarePanes(weights = List(panes.size) { 1f }, modifier = modifier.fillMaxSize()) {
        panes.forEach { pane ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                content = pane,
            )
        }
    }
}

private fun LazyListScope.overviewPane(
    state: VehicleUiState.Content,
    onUpdateMileage: () -> Unit,
    photoActions: VehiclePhotoActions,
) {
    item(key = "hero") { Hero(state, onUpdateMileage, photoActions) }
    item(key = "info") { InfoSection(state) }
    item(key = "sources") {
        val res = if (state.vehicle.registrationMode == RegistrationMode.Manual) {
            R.string.vehicle_sources_manual
        } else {
            R.string.vehicle_sources_auto
        }
        CardGroup(
            stringResource(R.string.vehicle_section_sources),
            Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        ) {
            ListRow(Icons.Filled.Info, stringResource(res))
        }
    }
}

private fun LazyListScope.recordsPane(
    state: VehicleUiState.Content,
    onInspectionDateSelected: (LocalDate?) -> Unit,
    onInspectionCompleted: (InspectionCompletion) -> Unit,
) {
    item(key = "official") {
        OfficialDataSection(state, onInspectionDateSelected, onInspectionCompleted)
    }
    item(key = "mileage-title") { SectionTitle(R.string.vehicle_section_mileage) }
    item(key = "mileage-log") { MileageLog(state.mileageLog) }
}

private fun LazyListScope.settingsEntry(onOpenSettings: () -> Unit) {
    item(key = "settings") {
        CardGroup(null, Modifier.padding(horizontal = ChageunTheme.spacing.gutter)) {
            ListRow(Icons.Filled.Settings, stringResource(R.string.vehicle_open_settings), onClick = onOpenSettings)
        }
    }
}

@Composable
private fun Hero(state: VehicleUiState.Content, onUpdateMileage: () -> Unit, photoActions: VehiclePhotoActions) {
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
        photoPath = state.photoPath,
        bodyType = vehicleBodyTypeOf(vehicle.model),
        mileage = current?.let { stringResource(R.string.vehicle_mileage, formatNumber(it.mileage.value)) },
        freshness = current?.let { stringResource(R.string.vehicle_mileage_as_of, formatDate(it.date)) }
            ?: stringResource(R.string.vehicle_mileage_none),
        action = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
                ) {
                    FilledTonalButton(onClick = onUpdateMileage) {
                        ButtonIcon(Icons.Filled.Speed)
                        Text(stringResource(R.string.vehicle_update_mileage))
                    }
                    PhotoButtons(state, photoActions)
                }
                if (state.isPhotoImportFailed) {
                    Text(
                        stringResource(R.string.vehicle_photo_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

@Composable
private fun PhotoButtons(state: VehicleUiState.Content, actions: VehiclePhotoActions) {
    OutlinedButton(onClick = actions.onPick) {
        ButtonIcon(Icons.Filled.AddAPhoto)
        Text(stringResource(if (state.photoPath == null) R.string.vehicle_photo_add else R.string.vehicle_photo_change))
    }
    if (state.photoPath != null) {
        TextButton(onClick = actions.onRemove) { Text(stringResource(R.string.vehicle_photo_remove)) }
    }
}

@Composable
private fun ButtonIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(ChageunTheme.spacing.xs))
}

@Composable
private fun InfoSection(state: VehicleUiState.Content) {
    val vehicle = state.vehicle
    val rows = listOf(
        Triple(Icons.Filled.Pin, R.string.vehicle_plate, vehicle.plateMasked),
        Triple(Icons.Filled.Factory, R.string.vehicle_maker, vehicle.maker),
        Triple(Icons.Filled.DirectionsCar, R.string.vehicle_model, vehicle.model),
        Triple(Icons.Filled.Style, R.string.vehicle_trim, vehicle.trim),
        Triple(Icons.Filled.CalendarMonth, R.string.vehicle_year, vehicle.modelYear?.toString()),
        Triple(
            vehicle.fuelType?.icon ?: Icons.Filled.LocalGasStation,
            R.string.vehicle_fuel,
            vehicle.fuelType?.let { stringResource(it.labelRes) },
        ),
        Triple(
            Icons.Filled.Event,
            R.string.vehicle_first_registration,
            vehicle.firstRegistrationDate?.let { formatDate(it) },
        ),
        Triple(
            Icons.Filled.EditNote,
            R.string.vehicle_registration_mode,
            stringResource(
                if (vehicle.registrationMode == RegistrationMode.Manual) {
                    R.string.vehicle_registration_manual
                } else {
                    R.string.vehicle_registration_auto
                },
            ),
        ),
    ).filterNot { it.third.isNullOrBlank() }
    CardGroup(
        stringResource(R.string.vehicle_section_info),
        Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
    ) {
        rows.forEachIndexed { index, (icon, labelRes, value) ->
            if (index > 0) GroupDivider()
            InfoListRow(icon, stringResource(labelRes), value)
        }
    }
}

/** 공식 데이터를 연동하기 전까지는 "리콜 없음"으로 보이지 않게 하고, 대신 공식 서비스를 안내한다. */
@Composable
private fun OfficialDataSection(
    state: VehicleUiState.Content,
    onInspectionDateSelected: (LocalDate?) -> Unit,
    onInspectionCompleted: (InspectionCompletion) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val analytics = LocalAnalyticsTracker.current
    val context = LocalContext.current
    Section(R.string.vehicle_section_official) {
        InspectionCard(
            status = state.inspection,
            currentMileage = state.currentMileage?.mileage,
            onDateSelected = onInspectionDateSelected,
            onCompleted = onInspectionCompleted,
        )
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Text(
                stringResource(R.string.vehicle_official_pending),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(ChageunTheme.spacing.md),
            )
        }
        CardGroup(null) {
            ListRow(
                icon = Icons.Filled.Campaign,
                title = stringResource(R.string.vehicle_official_recall),
                tone = ChageunTheme.colors.critical,
                onClick = {
                    analytics.track(AnalyticsEvent.RecallOpened)
                    uriHandler.openUriSafely(context, RECALL_CENTER_URL)
                },
                trailing = { ExternalIcon() },
            )
            GroupDivider()
            ListRow(
                icon = Icons.AutoMirrored.Filled.FactCheck,
                title = stringResource(R.string.vehicle_official_inspection),
                tone = ChageunTheme.colors.ai,
                onClick = { uriHandler.openUriSafely(context, INSPECTION_URL) },
                trailing = { ExternalIcon() },
            )
        }
    }
}

/** 최근 이력만 먼저 보여 주고, 나머지는 펼쳐서 본다. 대부분은 최근 몇 건만 확인한다. */
@Composable
private fun MileageLog(log: List<MileageEntry>) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (isExpanded) log else log.take(MILEAGE_PREVIEW)
    CardGroup(null, Modifier.padding(horizontal = ChageunTheme.spacing.gutter)) {
        shown.forEachIndexed { index, entry ->
            if (index > 0) GroupDivider()
            ListRow(
                icon = entry.source.icon,
                title = stringResource(R.string.vehicle_mileage, formatNumber(entry.mileage.value)),
                body = stringResource(entry.source.labelRes),
                trailing = {
                    Text(
                        formatDate(entry.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        if (log.size > MILEAGE_PREVIEW) {
            GroupDivider()
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ChageunTheme.spacing.xs),
            ) {
                Text(
                    if (isExpanded) {
                        stringResource(R.string.vehicle_mileage_show_less)
                    } else {
                        stringResource(R.string.vehicle_mileage_show_all, log.size)
                    },
                )
            }
        }
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

/** 앱 밖 웹사이트로 나간다는 표시. */
@Composable
private fun ExternalIcon() {
    Icon(
        Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

private val MileageSource.icon: ImageVector
    get() = when (this) {
        MileageSource.User -> Icons.Filled.Edit
        MileageSource.Maintenance -> Icons.Filled.Build
        MileageSource.Fuel -> Icons.Filled.LocalGasStation
        MileageSource.Check -> Icons.Filled.Handyman
        MileageSource.Correction -> Icons.Filled.Restore
        MileageSource.Inspection -> Icons.AutoMirrored.Filled.FactCheck
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

private const val MILEAGE_PREVIEW = 10
private const val RECALL_CENTER_URL = "https://www.car.go.kr"
private const val INSPECTION_URL = "https://www.cyberts.kr"
