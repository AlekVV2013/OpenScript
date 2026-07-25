package com.hlebushek.openscript

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutoIndexWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? App ?: return Result.failure()

        // Without the media permission there is nothing to index and retrying
        // would just burn battery until the user grants it from Settings.
        if (!app.photos.hasImagePermission()) {
            return Result.failure()
        }

        return try {
            app.photoIndexer.indexAllPhotos()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
