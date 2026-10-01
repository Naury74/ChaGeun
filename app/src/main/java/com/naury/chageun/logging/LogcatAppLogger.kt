package com.naury.chageun.logging

import android.util.Log
import com.naury.chageun.BuildConfig
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.common.logging.formatLogLine
import javax.inject.Inject

internal class LogcatAppLogger @Inject constructor() : AppLogger {

    override fun debug(event: String, vararg fields: LogField) {
        if (BuildConfig.DEBUG) Log.d(TAG, formatLogLine(event, fields, null))
    }

    override fun warn(event: String, vararg fields: LogField, error: Throwable?) {
        Log.w(TAG, formatLogLine(event, fields, error))
    }

    override fun error(event: String, vararg fields: LogField, error: Throwable?) {
        Log.e(TAG, formatLogLine(event, fields, error))
    }

    private companion object {
        const val TAG = "Chageun"
    }
}
