package com.example.freeassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                restoreScheduling(context)
            }
            "com.example.freeassistant.ACTION_INDEX_PHOTOS" -> {
                val app = context.applicationContext as App
                val time = SettingsManager.getAutoIndexTime(context)
                val parts = time.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: 2
                    val minute = parts[1].toIntOrNull() ?: 0
                    app.photos.indexAllPhotos()
                }
            }
        }
    }

    private fun restoreScheduling(context: Context) {
        if (SettingsManager.isAutoIndexEnabled(context)) {
            val time = SettingsManager.getAutoIndexTime(context)
            val parts = time.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 2
                val minute = parts[1].toIntOrNull() ?: 0
                AutoIndexScheduler.schedule(context, hour, minute)
            }
        }
    }
}
