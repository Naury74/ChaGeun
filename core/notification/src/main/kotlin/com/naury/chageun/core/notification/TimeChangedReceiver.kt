package com.naury.chageun.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 시간대나 시각이 바뀌면 알림 대상을 다시 평가한다. 시스템이 보내는 두 방송은 암시적 방송 제한에서 빠져 있어
 * 매니페스트에 등록한 리시버로 받을 수 있다. 실제 평가는 WorkManager 작업으로 넘겨 리시버를 짧게 끝낸다.
 */
internal class TimeChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        ReminderScheduler(context.applicationContext).reevaluateNow()
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)
    }
}
