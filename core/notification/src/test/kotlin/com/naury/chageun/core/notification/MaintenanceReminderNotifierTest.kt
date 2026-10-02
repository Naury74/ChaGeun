package com.naury.chageun.core.notification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.notification.DeepLinks.deepLinkOrNull
import java.time.LocalDate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MaintenanceReminderNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifier = MaintenanceReminderNotifier(context)
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun registerLauncherActivity() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = context.packageName
                name = "com.naury.chageun.MainActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(launcher, resolveInfo)
    }

    private fun status(state: MaintenanceState, km: Long?) = MaintenanceStatus(
        item = MaintenanceItem.EngineOil,
        state = state,
        remainingKm = km,
        remainingDays = 40,
        distanceDue = null,
        dateDue = null,
        estimatedDue = null,
        missingInputs = emptySet(),
        ruleSource = RuleSource.Generic,
    )

    @Test
    fun postsNotificationOnMaintenanceChannel_withRemainingDistance() {
        notifier.notify(listOf(status(MaintenanceState.Due, km = 420)))

        val posted = shadowOf(manager).allNotifications.single()
        assertThat(posted.channelId).isEqualTo(NotificationChannels.MAINTENANCE)
        assertThat(posted.extras.getString(NotificationCompat.EXTRA_TITLE)).isEqualTo("Time to replace your Engine oil")
        assertThat(posted.extras.getString(NotificationCompat.EXTRA_TEXT)).isEqualTo("About 420 km left")
    }

    @Test
    fun replacesPreviousNotificationForSameItem() {
        notifier.notify(listOf(status(MaintenanceState.Upcoming, km = 1_500)))
        notifier.notify(listOf(status(MaintenanceState.Overdue, km = -300)))

        val posted = shadowOf(manager).allNotifications.single()
        assertThat(posted.extras.getString(NotificationCompat.EXTRA_TEXT)).isEqualTo("300 km past the interval")
    }

    @Test
    fun opensCareTabItem_whenTapped() {
        notifier.notify(listOf(status(MaintenanceState.Due, km = 420)))

        val intent = shadowOf(shadowOf(manager).allNotifications.single().contentIntent).savedIntent
        assertThat(intent?.deepLinkOrNull()).isEqualTo(DeepLink.Maintenance(MaintenanceItem.EngineOil))
    }

    @Test
    fun inspection_postsOnInspectionChannel_andOpensMyCarTab() {
        val schedule = InspectionSchedule(LocalDate.of(2026, 10, 8), InspectionSource.User)

        notifier.notifyInspection(InspectionStatus(schedule, daysLeft = 7, state = InspectionState.DueSoon))

        val posted = shadowOf(manager).allNotifications.single()
        assertThat(posted.channelId).isEqualTo(NotificationChannels.INSPECTION)
        assertThat(posted.extras.getString(NotificationCompat.EXTRA_TITLE)).isEqualTo("Vehicle inspection is coming up")
        assertThat(posted.extras.getString(NotificationCompat.EXTRA_TEXT)).isEqualTo("Due in 7 days")
        assertThat(shadowOf(posted.contentIntent).savedIntent?.deepLinkOrNull()).isEqualTo(DeepLink.Inspection)
    }

    @Test
    fun inspection_overdue_saysHowLongAgo() {
        val schedule = InspectionSchedule(LocalDate.of(2026, 9, 28), InspectionSource.User)

        notifier.notifyInspection(InspectionStatus(schedule, daysLeft = -3, state = InspectionState.Overdue))

        val posted = shadowOf(manager).allNotifications.single()
        assertThat(posted.extras.getString(NotificationCompat.EXTRA_TITLE)).isEqualTo("Vehicle inspection is past due")
        assertThat(posted.extras.getString(NotificationCompat.EXTRA_TEXT)).isEqualTo("3 days past the deadline")
    }

    @Test
    fun mileagePrompt_opensMileageUpdate() {
        notifier.notifyMileagePrompt()

        val posted = shadowOf(manager).allNotifications.single()
        assertThat(posted.extras.getString(NotificationCompat.EXTRA_TITLE)).isEqualTo("Time to update your mileage")
        assertThat(shadowOf(posted.contentIntent).savedIntent?.deepLinkOrNull()).isEqualTo(DeepLink.MileageUpdate)
    }

    @Test
    fun createsSeparateChannelsPerKind() {
        NotificationChannels.ensureCreated(context)

        assertThat(manager.notificationChannels.map { it.id })
            .containsExactly(
                NotificationChannels.MAINTENANCE,
                NotificationChannels.INSPECTION,
                NotificationChannels.RECALL,
            )
    }
}
