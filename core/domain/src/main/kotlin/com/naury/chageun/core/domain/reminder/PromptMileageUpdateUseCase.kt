package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.domain.maintenance.MILEAGE_PROMPT_AFTER_DAYS
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** 기획서 14.3의 월 1회 주행거리 입력 알림. 사용자가 켠 경우에만 호출한다. */
class PromptMileageUpdateUseCase @Inject constructor(
    private val notifier: ReminderNotifier,
    private val log: MileageReminderLog,
    private val clock: Clock,
) {
    /**
     * 마지막 주행거리와 마지막 알림이 모두 [MILEAGE_PROMPT_AFTER_DAYS]일 이상 지났을 때만 알린다.
     * @return 알림을 띄웠으면 true.
     */
    suspend operator fun invoke(lastReadingOn: LocalDate?): Boolean {
        val today = LocalDate.now(clock)
        val readingIsStale = lastReadingOn == null || daysBetween(lastReadingOn, today) >= MILEAGE_PROMPT_AFTER_DAYS
        val lastNotified = log.lastNotifiedOn()
        val notifiedRecently = lastNotified != null && daysBetween(lastNotified, today) < MILEAGE_PROMPT_AFTER_DAYS
        if (!readingIsStale || notifiedRecently) return false
        notifier.notifyMileagePrompt()
        log.markNotified(today)
        return true
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)
}
