package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.reminder.ReminderNotifier
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceStatus

class FakeReminderNotifier : ReminderNotifier {
    var inspectionCancelCount = 0
    var mileagePromptCount = 0
    val cancelledItems = mutableListOf<MaintenanceItem>()

    override fun canNotify() = true

    override fun notify(statuses: List<MaintenanceStatus>) = Unit

    override fun cancel(item: MaintenanceItem) {
        cancelledItems += item
    }

    override fun notifyInspection(status: InspectionStatus) = Unit

    override fun cancelInspection() {
        inspectionCancelCount++
    }

    override fun notifyMileagePrompt() {
        mileagePromptCount++
    }
}
