package com.naury.chageun.feature.manage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.ServiceHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ManageViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    vehicleRepository: VehicleRepository,
    observeMaintenanceOverview: ObserveMaintenanceOverviewUseCase,
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, ManageFilter.All.name)
    private val selectedItem = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)

    val uiState: StateFlow<ManageUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            val history: Flow<List<ServiceHistoryEntry>> = selectedItem.flatMapLatest { name ->
                name?.let { maintenanceRepository.observeServiceHistory(vehicle.id, MaintenanceItem.valueOf(it)) }
                    ?: flowOf(emptyList())
            }
            combine(observeMaintenanceOverview(vehicle.id), filter, selectedItem, history) {
                    overview,
                    filterName,
                    selectedName,
                    entries,
                ->
                val activeFilter = ManageFilter.valueOf(filterName)
                val selected = selectedName?.let(MaintenanceItem::valueOf)
                val selectedStatus = overview.statuses.firstOrNull { it.item == selected }
                ManageUiState(
                    isLoading = false,
                    filter = activeFilter,
                    items = overview.statuses.filter(activeFilter::accepts),
                    counts = ManageFilter.entries.associateWith { f -> overview.statuses.count(f::accepts) },
                    rules = overview.rules,
                    selectedItem = selectedStatus?.item,
                    detail = selectedStatus?.let {
                        ManageDetail(it, overview.rules[it.item], entries, overview.currentMileage)
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ManageUiState())

    fun selectFilter(filter: ManageFilter) {
        savedStateHandle[KEY_FILTER] = filter.name
    }

    fun selectItem(item: MaintenanceItem?) {
        savedStateHandle[KEY_SELECTED] = item?.name
    }

    private companion object {
        const val KEY_FILTER = "manage_filter"
        const val KEY_SELECTED = "manage_selected_item"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
