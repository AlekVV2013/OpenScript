package com.hlebushek.openscript

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutoIndexWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as App
            app.photos.indexAllPhotos()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
