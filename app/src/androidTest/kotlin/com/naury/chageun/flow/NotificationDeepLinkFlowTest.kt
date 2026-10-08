package com.naury.chageun.flow

import android.content.Intent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.notification.DeepLinks.putMaintenanceItem
import com.naury.chageun.core.notification.DeepLinks.putMileageUpdate
import com.naury.chageun.feature.home.R as HomeR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 기획 문서 9.3 알림 Deep Link. 알림의 PendingIntent와 같은 Intent로 실행 중인 앱을 다시 열어
 * onNewIntent 경로에서 알림이 가리키는 화면이 열리는지 본다.
 */
@RunWith(AndroidJUnit4::class)
class NotificationDeepLinkFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { AppRobot(rule) }

    @Test
    fun maintenanceReminder_opensItemDetailInCareTab() {
        robot.registerVehicle()
        val item = MaintenanceItem.Tire

        openFromNotification { putMaintenanceItem(item) }

        robot.waitFor(hasText(robot.itemName(item)) and isHeading())
        robot.waitFor(robot.recordActionButton)
    }

    @Test
    fun mileageReminder_opensMileageSheet() {
        robot.registerVehicle()

        openFromNotification { putMileageUpdate() }

        robot.waitFor(hasSetTextAction() and hasText(robot.text(HomeR.string.home_mileage_update_label)))
    }

    /** MaintenanceReminderNotifier가 만드는 Intent와 같은 실행 Intent·플래그로 앱을 연다. */
    private fun openFromNotification(deepLink: Intent.() -> Intent) {
        val context = rule.activity
        val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            .deepLink()
        context.startActivity(intent)
    }
}
