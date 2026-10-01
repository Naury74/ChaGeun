package com.naury.chageun.core.database

import androidx.room.migration.Migration

/**
 * Every schema change ships a manual or auto migration. Destructive fallback is never enabled,
 * so a missing entry here crashes on upgrade instead of silently wiping user records.
 */
object DatabaseMigrations {
    val ALL: List<Migration> = emptyList()
}
