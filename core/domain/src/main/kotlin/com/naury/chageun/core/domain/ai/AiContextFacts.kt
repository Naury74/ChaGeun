package com.naury.chageun.core.domain.ai

import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleHealthLevel
import java.time.LocalDate

/**
 * 외부 AI에 공유할 수 있는 정보 전체. 차량 번호, VIN, 소유자 이름, 위치, 메모, 첨부 필드는 일부러 두지 않아
 * 렌더링 실수로도 새어 나갈 수 없게 한다.
 */
data class AiContextFacts(
    val asOf: LocalDate,
    val maker: String,
    val model: String,
    val modelYear: Int?,
    val fuelType: FuelType?,
    val mileage: MileageReading?,
    val health: VehicleHealthLevel,
    val maintenance: List<MaintenanceStatus>,
    val missingInfo: List<MaintenanceItem>,
    val recentRecords: List<SharedRecord>,
    /** 기록 상세에서 물어볼 때 질문 대상인 기록. 최근 기록 목록에는 다시 넣지 않는다. */
    val focusRecord: SharedRecord? = null,
    /** 다음 정기검사 일정. 날짜를 모르면 null이다. */
    val inspection: InspectionStatus? = null,
    /** 비용을 함께 보낼 때만 채운다. 최근 기록 몇 개로는 합계를 물을 수 없어 앱이 계산해 둔다. */
    val costTotals: CostTotals? = null,
)

/** [year] 1월 1일부터 오늘까지 기록된 비용 합계. 비용을 입력하지 않은 기록은 0으로 센다. */
data class CostTotals(val year: Int, val maintenanceWon: Long, val fuelWon: Long)

/**
 * 질문에 답하는 데 필요한 정보만 남긴 이력 항목. 메모와 주유소 이름은 절대 넣지 않는다.
 * 메모는 자유 텍스트이고, 주유소 이름은 사용자가 다니는 장소를 드러내기 때문이다.
 */
data class SharedRecord(
    val type: TimelineEventType,
    val date: LocalDate?,
    val maintenanceItem: MaintenanceItem?,
    val title: String?,
    val mileage: Kilometers?,
    val costWon: Long?,
    /** 질문 대상 주유 기록에만 채운다. 연비나 단가를 물을 때 필요하다. */
    val fuelVolumeMl: Long? = null,
    val fuelUnitPriceWon: Long? = null,
    val isFullTank: Boolean? = null,
)

data class AiContextOptions(
    val includeMaintenance: Boolean = true,
    val includeRecords: Boolean = true,
    val includeCosts: Boolean = false,
    /** 값이 있으면 이 항목의 정비 상태만 공유한다. */
    val focusItem: MaintenanceItem? = null,
    /** 기록 상세에서 열면 그 기록. 정비 기록이면 그 항목의 상태와 기록도 함께 공유한다. */
    val focusRecord: RecordRef? = null,
)
