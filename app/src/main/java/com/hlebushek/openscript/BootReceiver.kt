package com.hlebushek.openscript

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the daily indexing schedule after events that clear or invalidate it.
 *
 * The indexing itself runs in [AutoIndexWorker]; a broadcast receiver may only run
 * for a few seconds, which is nowhere near enough to label a photo library.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                AutoIndexScheduler.schedule(context.applicationContext)
            }
        }
    }
}
