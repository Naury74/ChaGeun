package com.naury.chageun.feature.onboarding

import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.ServiceRecord
import java.time.LocalDate

/**
 * 마지막 교체 시기. 대부분은 정확한 날짜를 기억하지 못하므로 대략적인 시기를 고르게 하고,
 * 영수증이 있는 사람만 [Exact]로 날짜와 주행거리를 넣는다.
 * [monthsAgo]는 고른 시기의 대표값이다. 대략적인 값이라도 '모름'보다 다음 교체 시점을 훨씬 잘 추정한다.
 */
enum class QuickServiceMode(val monthsAgo: Long? = null) {
    Unknown,
    Recent(monthsAgo = 2),
    HalfYear(monthsAgo = 6),
    OneYear(monthsAgo = 12),
    Exact,
}

data class QuickServiceInput(
    val mode: QuickServiceMode = QuickServiceMode.Unknown,
    val date: LocalDate? = null,
    val mileage: String = "",
)

/** 온보딩에서 묻는 세 항목. 나머지는 나중에 Home에서 채운다. */
val QUICK_SERVICE_ITEMS = listOf(MaintenanceItem.EngineOil, MaintenanceItem.Tire, MaintenanceItem.Battery)

internal object QuickServiceForm {

    /** 대략적인 시기를 고르면 날짜를 바로 채우고, [QuickServiceMode.Exact]는 사용자가 고른 날짜를 기다린다. */
    fun select(input: QuickServiceInput, mode: QuickServiceMode, today: LocalDate): QuickServiceInput = when (mode) {
        QuickServiceMode.Unknown -> QuickServiceInput()
        QuickServiceMode.Exact -> input.copy(mode = mode, date = input.date.takeIf { input.mode == mode })
        else -> QuickServiceInput(mode = mode, date = today.minusMonths(checkNotNull(mode.monthsAgo)))
    }

    fun validate(
        inputs: Map<MaintenanceItem, QuickServiceInput>,
        currentMileage: Long,
        today: LocalDate,
    ): Map<MaintenanceItem, FieldError> = buildMap {
        inputs.forEach { (item, input) ->
            val date = input.date
            val mileage = input.mileage.toLongOrNull()
            val error = when {
                input.mode != QuickServiceMode.Exact -> null
                date == null -> FieldError.Required
                date.isAfter(today) -> FieldError.FutureDate
                mileage != null && mileage > currentMileage -> FieldError.ExceedsCurrentMileage
                else -> null
            }
            error?.let { put(item, it) }
        }
    }

    fun toServiceRecords(inputs: Map<MaintenanceItem, QuickServiceInput>): Map<MaintenanceItem, ServiceRecord> = inputs
        .filterValues { it.mode != QuickServiceMode.Unknown }
        .mapValues { (_, input) ->
            ServiceRecord(
                date = input.date,
                mileage = input.mileage.toLongOrNull()?.takeIf {
                    input.mode == QuickServiceMode.Exact
                }?.let(::Kilometers),
            )
        }
}
