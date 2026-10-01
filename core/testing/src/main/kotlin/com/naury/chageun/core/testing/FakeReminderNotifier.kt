package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.reminder.ReminderNotifier
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceStatus

class FakeReminderNotifier : ReminderNotifier {
    var inspectionCancelCount = 0

    override fun canNotify() = true

    override fun notify(statuses: List<MaintenanceStatus>) = Unit

    override fun notifyInspection(status: InspectionStatus) = Unit

    override fun cancelInspection() {
        inspectionCancelCount++
    }
}
