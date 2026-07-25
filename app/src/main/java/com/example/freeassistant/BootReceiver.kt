package com.example.freeassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as App
                        app.photos.indexAllPhotos()
                    } catch (_: Exception) {
                        // Ignore indexing failures from the background broadcast.
                    } finally {
                        pendingResult.finish()
                    }
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
