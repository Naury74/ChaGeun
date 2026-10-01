package com.naury.chageun.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.naury.chageun.core.database.converter.TimeConverters
import com.naury.chageun.core.database.dao.AttachmentDao
import com.naury.chageun.core.database.dao.BackupDao
import com.naury.chageun.core.database.dao.HistoryDao
import com.naury.chageun.core.database.dao.InspectionDao
import com.naury.chageun.core.database.dao.MaintenanceDao
import com.naury.chageun.core.database.dao.MileageRecordDao
import com.naury.chageun.core.database.dao.ReminderDao
import com.naury.chageun.core.database.dao.VehicleDao
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.InspectionScheduleEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.ReminderStateEntity
import com.naury.chageun.core.database.entity.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        MileageRecordEntity::class,
        MaintenanceRuleEntity::class,
        MaintenanceRecordEntity::class,
        FuelRecordEntity::class,
        CheckRecordEntity::class,
        AttachmentEntity::class,
        ReminderStateEntity::class,
        InspectionScheduleEntity::class,
    ],
    version = ChageunDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(TimeConverters::class)
abstract class ChageunDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    abstract fun mileageRecordDao(): MileageRecordDao

    abstract fun maintenanceDao(): MaintenanceDao

    abstract fun historyDao(): HistoryDao

    abstract fun attachmentDao(): AttachmentDao

    abstract fun reminderDao(): ReminderDao

    abstract fun backupDao(): BackupDao

    abstract fun inspectionDao(): InspectionDao

    companion object {
        const val NAME = "chageun.db"
        const val VERSION = 6
    }
}
