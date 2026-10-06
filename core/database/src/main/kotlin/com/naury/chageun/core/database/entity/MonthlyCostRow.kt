package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo

/** [month]는 'YYYY-MM'이며 날짜 없는 기록의 합계는 null이다. 비용을 입력한 기록이 없으면 [totalWon]도 null이다. */
data class MonthlyCostRow(
    @ColumnInfo(name = "month") val month: String?,
    @ColumnInfo(name = "total_won") val totalWon: Long?,
)
