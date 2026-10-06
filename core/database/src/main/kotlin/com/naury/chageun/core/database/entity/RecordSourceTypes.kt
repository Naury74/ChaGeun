package com.naury.chageun.core.database.entity

/** `maintenance_record.source_type` 값. 문자열로 저장하므로 이름을 바꾸면 기존 데이터를 읽지 못한다. */
object RecordSourceTypes {
    const val USER = "USER"

    /** 날짜만 알고 주행거리는 평균 주행량으로 추정한 기록. */
    const val ESTIMATED = "ESTIMATED"
}
