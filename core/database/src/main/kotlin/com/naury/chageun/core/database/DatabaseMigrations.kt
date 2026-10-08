package com.naury.chageun.core.database

import androidx.room.migration.Migration
import com.naury.chageun.core.database.migration.Migration1To2
import com.naury.chageun.core.database.migration.Migration2To3
import com.naury.chageun.core.database.migration.Migration3To4
import com.naury.chageun.core.database.migration.Migration4To5
import com.naury.chageun.core.database.migration.Migration5To6
import com.naury.chageun.core.database.migration.Migration6To7
import com.naury.chageun.core.database.migration.Migration7To8

/**
 * 스키마를 바꿀 때마다 수동 또는 자동 Migration을 함께 넣는다. Destructive fallback은 절대 켜지 않으므로
 * 여기 항목이 빠지면 사용자 기록을 조용히 지우는 대신 업그레이드 시점에 크래시가 난다.
 */
object DatabaseMigrations {
    val ALL: List<Migration> = listOf(
        Migration1To2,
        Migration2To3,
        Migration3To4,
        Migration4To5,
        Migration5To6,
        Migration6To7,
        Migration7To8,
    )
}
