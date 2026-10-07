package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.HistoryEntryError
import com.naury.chageun.core.domain.reminder.ReminderNotifier
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.PeriodicInspectionResult
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.absoluteValue

class CompleteInspectionUseCase @Inject constructor(
    private val addHistoryRecord: AddHistoryRecordUseCase,
    private val inspectionRepository: InspectionRepository,
    private val notifier: ReminderNotifier,
) {
    /**
     * 검사를 Timeline에 기록하고 [nextDueDate]를 저장한다(null이면 지운다).
     * 기록이 유효할 때만 날짜를 바꾸므로, 거부된 입력은 기존 일정을 그대로 둔다.
     * 부적합이어도 검사를 받은 기록은 남긴다. 재검사 일정은 사용자가 다음 검사일로 고친다.
     */
    suspend operator fun invoke(
        vehicleId: VehicleId,
        completedOn: LocalDate,
        title: String,
        mileage: Kilometers?,
        result: PeriodicInspectionResult,
        nextDueDate: LocalDate?,
    ): Set<HistoryEntryError> {
        val errors = addHistoryRecord.addCheck(
            vehicleId,
            CheckEntry(
                kind = CheckKind.Inspection,
                date = completedOn,
                title = title,
                mileage = mileage,
                periodicResult = result,
            ),
        )
        if (errors.isNotEmpty()) return errors
        inspectionRepository.setUserDueDate(vehicleId, nextDueDate)
        notifier.cancelInspection()
        return errors
    }

    companion object {
        /** 비사업용 승용차는 최초 검사 이후 2년마다 검사한다. */
        private const val INTERVAL_YEARS = 2L

        /** 만료일 전후 이 일수 안에 받은 검사는 원래 주기를 유지한다. */
        private const val ON_TIME_WINDOW_DAYS = 31L

        /**
         * 사용자가 안내문과 대조해 확인할 기준값이다. 기간 내 검사는 이전 만료일에서 연장하고,
         * 늦거나 이른 검사는 검사일부터 계산한다.
         */
        fun suggestNextDueDate(completedOn: LocalDate, previousDueDate: LocalDate?): LocalDate {
            val onTime = previousDueDate != null &&
                ChronoUnit.DAYS.between(previousDueDate, completedOn).absoluteValue <= ON_TIME_WINDOW_DAYS
            val base = if (onTime) previousDueDate else completedOn
            return base.plusYears(INTERVAL_YEARS)
        }
    }
}
