package com.hlebushek.openscript

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily photo indexing pass.
 *
 * This uses WorkManager rather than AlarmManager + a BroadcastReceiver: labelling a
 * whole gallery with ML Kit takes far longer than the ~10s a broadcast receiver is
 * allowed to run, and WorkManager survives reboots and process death on its own.
 */
object AutoIndexScheduler {
    private const val WORK_NAME = "auto_index_photos"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<AutoIndexWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMinutes(hour, minute), TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Schedules using the stored time when auto-indexing is enabled, otherwise cancels.
     */
    fun schedule(context: Context) {
        if (SettingsManager.isAutoIndexEnabled(context)) {
            schedule(
                context,
                SettingsManager.getAutoIndexHour(context),
                SettingsManager.getAutoIndexMinute(context)
            )
        } else {
            cancel(context)
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Minutes from now until the next occurrence of [hour]:[minute]. */
    private fun initialDelayMinutes(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (next.timeInMillis - now.timeInMillis) / 60_000L
    }
}
