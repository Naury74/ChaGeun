package com.naury.chageun.core.domain.reminder

import java.time.LocalDate

/** 주행거리 입력 알림을 마지막으로 보낸 날. 월 1회 제한에만 쓰므로 차량별로 나누지 않는다. */
interface MileageReminderLog {
    suspend fun lastNotifiedOn(): LocalDate?

    suspend fun markNotified(date: LocalDate)
}
