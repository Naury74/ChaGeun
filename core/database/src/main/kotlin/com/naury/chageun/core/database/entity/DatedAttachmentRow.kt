package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import java.time.LocalDate

/** 첨부와 그 첨부가 달린 기록의 날짜. 달력은 첨부를 찍은 시각이 아니라 기록 날짜에 놓는다. */
data class DatedAttachmentRow(
    @Embedded val attachment: AttachmentEntity,
    @ColumnInfo(name = "occurred_on") val occurredOn: LocalDate,
)
