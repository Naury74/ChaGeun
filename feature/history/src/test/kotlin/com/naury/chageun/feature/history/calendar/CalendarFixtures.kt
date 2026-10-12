package com.naury.chageun.feature.history.calendar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import com.naury.chageun.core.domain.calendar.ObserveCalendarMonthUseCase
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.CalendarDay
import com.naury.chageun.core.model.CalendarMonth
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordPhoto
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * 2026년 10월. 오늘(12일)에 기록·앨범 사진·영수증·예정이 모두 있고, 8일은 기록이 넘쳐 +n이 붙으며
 * 엔진오일 교체 기록에 붙인 사진만 있다.
 */
internal object CalendarFixtures {
    val TODAY: LocalDate = LocalDate.of(2026, 10, 12)
    val OCTOBER: YearMonth = YearMonth.of(2026, 10)

    fun record(
        id: String,
        type: TimelineEventType,
        date: LocalDate,
        title: String? = null,
        item: MaintenanceItem? = null,
        cost: Long? = null,
        km: Long = 41_000,
    ) = TimelineItem(RecordRef(type, id), date, title, item, Kilometers(km), cost, RecordSource.User, Instant.EPOCH)

    fun maintenance(id: String, date: LocalDate, item: MaintenanceItem) =
        record(id, TimelineEventType.Maintenance, date, item = item)

    fun photo(id: String, date: LocalDate, path: String = "/missing/$id.jpg") =
        AlbumPhoto(id, path, path, date, null, Instant.EPOCH)

    val todayRecords = listOf(
        record("fuel-today", TimelineEventType.Fuel, TODAY, title = "GS Caltex", cost = 68_000, km = 41_820),
        record("wash", TimelineEventType.Note, TODAY, title = "Car wash", cost = 15_000),
    )

    val overdueWiper = UpcomingEvent.MaintenanceDue(MaintenanceItem.Wiper, TODAY, false, null, isOverdue = true)
    val cabinFilterDue = UpcomingEvent.MaintenanceDue(MaintenanceItem.CabinFilter, TODAY, false, null)
    val inspectionDue = UpcomingEvent.InspectionDue(LocalDate.of(2026, 10, 24))
    val oilEstimate = UpcomingEvent.MaintenanceDue(
        MaintenanceItem.EngineOil,
        LocalDate.of(2026, 10, 27),
        isEstimated = true,
        confidence = Confidence.Medium,
    )
    val airFilterEstimate = UpcomingEvent.MaintenanceDue(
        MaintenanceItem.AirFilter,
        LocalDate.of(2026, 10, 30),
        isEstimated = true,
        confidence = Confidence.Low,
    )
    val mileageUpdate = UpcomingEvent.MileageUpdate(LocalDate.of(2026, 11, 11))

    fun recordPhoto(id: String, owner: RecordRef, date: LocalDate, path: String = "/missing/$id.jpg") =
        RecordPhoto(Attachment(id, owner, path, path, Instant.EPOCH), date)

    fun month(photoPaths: Map<String, String> = emptyMap()): CalendarMonth {
        val (start, end) = ObserveCalendarMonthUseCase.gridRange(OCTOBER, DayOfWeek.SUNDAY)
        fun pic(id: String, date: LocalDate) = photo(id, date, photoPaths[id] ?: "/missing/$id.jpg")
        fun receipt(id: String, owner: RecordRef, date: LocalDate) =
            recordPhoto(id, owner, date, photoPaths[id] ?: "/missing/$id.jpg")
        val oct3 = LocalDate.of(2026, 10, 3)
        val oct8 = LocalDate.of(2026, 10, 8)
        val days = listOf(
            CalendarDay(
                LocalDate.of(2026, 9, 28),
                records = listOf(
                    record("sep-fuel", TimelineEventType.Fuel, LocalDate.of(2026, 9, 28), "SK", cost = 62_000),
                ),
            ),
            CalendarDay(
                oct3,
                records = listOf(
                    record("fuel-3", TimelineEventType.Fuel, oct3, title = "S-Oil", cost = 70_000, km = 41_200),
                    record("mileage-3", TimelineEventType.Mileage, oct3, km = 41_200),
                ),
                photos = listOf(pic("road-trip", oct3)),
            ),
            CalendarDay(
                oct8,
                records = listOf(
                    record("oil", TimelineEventType.Maintenance, oct8, item = MaintenanceItem.EngineOil, cost = 95_000),
                    record("oil-filter", TimelineEventType.Maintenance, oct8, item = MaintenanceItem.OilFilter),
                    record("air", TimelineEventType.Maintenance, oct8, item = MaintenanceItem.AirFilter, cost = 18_000),
                    record("tire-check", TimelineEventType.Inspection, oct8, title = "Tire check"),
                    record("memo", TimelineEventType.Note, oct8, title = "Noise"),
                ),
                recordPhotos = listOf(receipt("oil-receipt", RecordRef(TimelineEventType.Maintenance, "oil"), oct8)),
            ),
            CalendarDay(
                TODAY,
                todayRecords,
                listOf(pic("today", TODAY)),
                listOf(overdueWiper, cabinFilterDue),
                recordPhotos = listOf(receipt("fuel-receipt", todayRecords.first().ref, TODAY)),
            ),
            CalendarDay(
                LocalDate.of(2026, 10, 20),
                records = listOf(
                    record("brake", TimelineEventType.Repair, LocalDate.of(2026, 10, 20), "Brake check", cost = 45_000),
                ),
            ),
            CalendarDay(inspectionDue.date, upcoming = listOf(inspectionDue)),
            CalendarDay(oilEstimate.date, upcoming = listOf(oilEstimate)),
            CalendarDay(airFilterEstimate.date, upcoming = listOf(airFilterEstimate)),
        ).associateBy { it.date }
        return CalendarMonth(
            month = OCTOBER,
            firstDayOfWeek = DayOfWeek.SUNDAY,
            gridStart = start,
            gridEnd = end,
            today = TODAY,
            days = days,
            monthTotalWon = 311_000,
            recordCount = 10,
            photoCount = 4,
        )
    }

    fun state(
        selected: LocalDate = TODAY,
        photoPaths: Map<String, String> = emptyMap(),
        mode: CalendarMode? = null,
        filter: CalendarFilter = CalendarFilter(),
        search: CalendarSearch = CalendarSearch(),
    ) = CalendarUiState(
        isLoading = false,
        calendar = month(photoPaths),
        selectedDate = selected,
        mode = mode,
        filter = filter,
        search = search,
        upcomingSoon = listOf(inspectionDue, oilEstimate, airFilterEstimate, mileageUpdate),
    )

    /** "oil"로 찾은 결과. 8일의 엔진오일·오일필터 교체와 지난 7월 엔진오일 교체가 걸린다. */
    val oilSearch = CalendarSearch(
        isOpen = true,
        keyword = "oil",
        results = listOf(
            maintenance("oil", LocalDate.of(2026, 10, 8), MaintenanceItem.EngineOil),
            maintenance("oil-filter", LocalDate.of(2026, 10, 8), MaintenanceItem.OilFilter),
            maintenance("oil-july", LocalDate.of(2026, 7, 2), MaintenanceItem.EngineOil),
        ),
    )

    /** 스크린샷용 사진. 실제 사진 대신 하늘·도로 두 색으로 나눈 이미지를 쓴다. */
    fun photoFiles(): Map<String, String> = mapOf(
        "road-trip" to writePhoto(Color.rgb(176, 205, 230), Color.DKGRAY),
        "today" to writePhoto(Color.rgb(198, 226, 190), Color.rgb(90, 110, 80)),
        "oil-receipt" to writePhoto(Color.rgb(245, 243, 236), Color.rgb(200, 196, 186)),
        "fuel-receipt" to writePhoto(Color.rgb(240, 240, 240), Color.rgb(190, 190, 190)),
    )

    private fun writePhoto(sky: Int, ground: Int): String {
        val file = File.createTempFile("calendar", ".jpg").apply { deleteOnExit() }
        val bitmap = Bitmap.createBitmap(PHOTO_SIZE, PHOTO_SIZE, Bitmap.Config.ARGB_8888).applyCanvas {
            drawColor(sky)
            drawRect(
                0f,
                PHOTO_SIZE * GROUND_FROM,
                PHOTO_SIZE.toFloat(),
                PHOTO_SIZE.toFloat(),
                Paint().apply {
                    color =
                        ground
                },
            )
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        // 네이티브 그래픽의 JNI 준비를 테스트 스레드에서 먼저 끝낸다. 처음 디코딩이 IO 스레드에서 일어나면
        // Robolectric이 java.nio 클래스를 찾지 못하고 프로세스를 멈춘다.
        BitmapFactory.decodeFile(file.absolutePath)
        return file.absolutePath
    }

    private const val PHOTO_SIZE = 240
    private const val GROUND_FROM = 0.6f
    private const val JPEG_QUALITY = 90
}
