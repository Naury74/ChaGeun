package com.naury.chageun.core.database

import androidx.room.migration.Migration
import com.naury.chageun.core.database.migration.Migration1To2
import com.naury.chageun.core.database.migration.Migration2To3

/**
 * Every schema change ships a manual or auto migration. Destructive fallback is never enabled,
 * so a missing entry here crashes on upgrade instead of silently wiping user records.
 */
object DatabaseMigrations {
    val ALL: List<Migration> = listOf(Migration1To2, Migration2To3)
}
