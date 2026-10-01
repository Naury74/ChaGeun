package com.naury.chageun.core.notification

import com.naury.chageun.core.domain.reminder.ReminderNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NotificationModule {
    @Binds
    fun bindReminderNotifier(notifier: MaintenanceReminderNotifier): ReminderNotifier
}
