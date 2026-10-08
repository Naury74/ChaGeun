package com.naury.chageun.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 정기검사 결과 칸을 추가한다. 7버전까지 "검사 받음"은 제목만 붙여 일반 점검과 같은 기록을 남겼으므로,
 * 그때 붙이던 제목 그대로인 점검 기록을 결과 미기록 정기검사로 옮긴다.
 */
internal object Migration7To8 : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `check_record` ADD COLUMN `periodic_result` TEXT")
        // vehicle_inspection_record_title의 7버전 당시 한국어·영어 값이다. 리소스 문구를 바꿔도 여기는 그대로 둔다.
        connection.execSQL(
            "UPDATE `check_record` SET `periodic_result` = 'Unknown' " +
                "WHERE `kind` = 'Inspection' AND `title` IN ('자동차 정기검사', 'Periodic inspection')",
        )
    }
}
