package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.reminder.ReminderNotifier
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class ServiceEntryError { FutureDate, NegativeCost }

sealed interface RecordServiceResult {
    /** [alsoReplaced]는 같은 날짜·주행거리로 함께 저장한 다른 항목이다. */
    data class Saved(
        val nextDistanceDue: Kilometers?,
        val nextDateDue: LocalDate?,
        val alsoReplaced: List<MaintenanceItem> = emptyList(),
    ) : RecordServiceResult

    data class Rejected(val errors: Set<ServiceEntryError>) : RecordServiceResult

    /** 같은 항목의 이전 정비보다 낮은 값이다. 유지하려면 확인 후 다시 저장한다. */
    data class NeedsConfirmation(val previousMileage: Kilometers) : RecordServiceResult
}

class RecordServiceUseCase @Inject constructor(
    private val repository: MaintenanceRepository,
    private val clock: Clock,
    private val analytics: AnalyticsTracker,
    private val notifier: ReminderNotifier,
) {
    /** 입력 폼의 주행거리 기본값. */
    suspend fun currentMileage(vehicleId: VehicleId): Kilometers? = repository.findCurrentMileage(vehicleId)?.mileage

    /**
     * [item]과 함께 기록할 수 있는 항목. 꺼 둔 항목과 [item] 자신은 빼고, 보통 같이 바꾸는 항목을 앞에 둔다.
     * 나머지는 정비 항목 순서를 따른다.
     */
    suspend fun companionCandidates(vehicleId: VehicleId, item: MaintenanceItem): List<MaintenanceItem> {
        val usual = USUALLY_REPLACED_TOGETHER[item].orEmpty()
        return repository.observeInputs(vehicleId).first().rules
            .filter { it.isEnabled && it.item != item }
            .map { it.item }
            .sortedWith(compareBy<MaintenanceItem> { it !in usual }.thenBy { it.ordinal })
    }

    suspend operator fun invoke(
        vehicleId: VehicleId,
        entry: ServiceEntry,
        isLowerMileageConfirmed: Boolean = false,
        alsoReplaced: Set<MaintenanceItem> = emptySet(),
    ): RecordServiceResult {
        val errors = buildSet {
            if (entry.date.isAfter(LocalDate.now(clock))) add(ServiceEntryError.FutureDate)
            if ((entry.costWon ?: 0) < 0) add(ServiceEntryError.NegativeCost)
        }
        if (errors.isNotEmpty()) return RecordServiceResult.Rejected(errors)

        val previousMileage = repository.findLatestService(vehicleId, entry.item)?.mileage
        if (!isLowerMileageConfirmed && previousMileage != null && entry.mileage < previousMileage) {
            return RecordServiceResult.NeedsConfirmation(previousMileage)
        }

        val currentMileage = repository.findCurrentMileage(vehicleId)?.mileage
        val advancesOdometer = currentMileage == null || entry.mileage > currentMileage
        repository.recordService(vehicleId, entry, advancesOdometer)
        analytics.track(AnalyticsEvent.MaintenanceRecordAdded(withCost = entry.costWon != null))
        notifier.cancel(entry.item)
        // 함께 바꾼 항목은 같은 날짜·주행거리로 따로 기록해 각자의 다음 교체 시점을 다시 계산한다.
        // 비용·메모는 한 번만 들어가야 지출 합계가 부풀지 않으므로 첫 항목에만 남긴다.
        val companions = (alsoReplaced - entry.item).sortedBy { it.ordinal }
        companions.forEach { companion ->
            repository.recordService(
                vehicleId,
                entry.copy(item = companion, costWon = null, memo = null),
                advancesOdometer = false,
            )
            notifier.cancel(companion)
        }

        val rule = repository.findRule(vehicleId, entry.item)
        return RecordServiceResult.Saved(
            nextDistanceDue = rule?.intervalKm?.let { entry.mileage + Kilometers(it) },
            nextDateDue = rule?.intervalMonths?.let { entry.date.plusMonths(it) },
            alsoReplaced = companions,
        )
    }
}

/** 정비소에서 보통 한 번에 바꾸는 항목. 함께 교체할 항목을 고를 때 앞에 보여 준다. */
private val USUALLY_REPLACED_TOGETHER: Map<MaintenanceItem, Set<MaintenanceItem>> = mapOf(
    MaintenanceItem.EngineOil to setOf(MaintenanceItem.OilFilter, MaintenanceItem.AirFilter),
    MaintenanceItem.OilFilter to setOf(MaintenanceItem.EngineOil),
    MaintenanceItem.AirFilter to setOf(MaintenanceItem.CabinFilter, MaintenanceItem.EngineOil),
    MaintenanceItem.CabinFilter to setOf(MaintenanceItem.AirFilter),
    MaintenanceItem.BrakePad to setOf(MaintenanceItem.BrakeFluid),
    MaintenanceItem.BrakeFluid to setOf(MaintenanceItem.BrakePad),
    MaintenanceItem.Coolant to setOf(MaintenanceItem.TransmissionOil),
    MaintenanceItem.TransmissionOil to setOf(MaintenanceItem.Coolant),
)
