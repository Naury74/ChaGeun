package com.naury.chageun.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.naury.chageun.core.database.converter.TimeConverters
import com.naury.chageun.core.database.dao.MaintenanceDao
import com.naury.chageun.core.database.dao.MileageRecordDao
import com.naury.chageun.core.database.dao.VehicleDao
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        MileageRecordEntity::class,
        MaintenanceRuleEntity::class,
        MaintenanceRecordEntity::class,
    ],
    version = ChageunDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(TimeConverters::class)
abstract class ChageunDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    abstract fun mileageRecordDao(): MileageRecordDao

    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        const val NAME = "chageun.db"
        const val VERSION = 1
    }
}
