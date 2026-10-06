package com.naury.chageun.core.notification

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimeChangedReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    private fun queuedReevaluations() =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork("reminder-on-start").get()

    @Test
    fun timezoneChange_queuesReevaluation() {
        TimeChangedReceiver().onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))

        assertThat(queuedReevaluations()).hasSize(1)
    }

    @Test
    fun clockChange_queuesReevaluation() {
        TimeChangedReceiver().onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        assertThat(queuedReevaluations()).hasSize(1)
    }

    @Test
    fun otherBroadcasts_areIgnored() {
        TimeChangedReceiver().onReceive(context, Intent(Intent.ACTION_BATTERY_LOW))

        assertThat(queuedReevaluations()).isEmpty()
    }
}
