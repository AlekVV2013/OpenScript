package com.example.freeassistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AutoIndexScheduler {
    private const val ALARM_REQUEST_CODE = 1001

    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = "com.example.freeassistant.ACTION_INDEX_PHOTOS"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        val alarmTime = calendar.timeInMillis
        val triggerTime = if (alarmTime <= now) {
            alarmTime + 24 * 60 * 60 * 1000
        } else {
            alarmTime
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            24 * 60 * 60 * 1000,
            pendingIntent
        )
    }

    // Compatibility overload used by newer MainActivity template: schedule with forceRecreate flag
    fun schedule(context: Context, forceRecreate: Boolean = false) {
        // If auto-index is enabled, schedule using stored time, otherwise do nothing
        if (SettingsManager.isAutoIndexEnabled(context)) {
            schedule(
                context,
                SettingsManager.getAutoIndexHour(context),
                SettingsManager.getAutoIndexMinute(context)
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = "com.example.freeassistant.ACTION_INDEX_PHOTOS"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
