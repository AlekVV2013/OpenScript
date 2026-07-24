package com.example.freeassistant

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.freeassistant.photos.PhotoRepository

class AutoIndexWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val app = context.applicationContext as App
            val photoRepo = app.photos
            photoRepo.indexAllPhotos()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
