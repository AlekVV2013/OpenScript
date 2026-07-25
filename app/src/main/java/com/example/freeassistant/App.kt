package com.example.freeassistant

import android.app.Application
import com.example.freeassistant.notes.NotesRepository
import com.example.freeassistant.photos.DescriptionEngine
import com.example.freeassistant.photos.PhotoRepository
import com.google.android.material.color.DynamicColors

class App : Application() {
    val photos by lazy { PhotoRepository(this) }
    val descriptions by lazy { DescriptionEngine(this) }
    val notes by lazy { NotesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Material You: use wallpaper-based colors on Android 12+ when available.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Restore auto-index scheduling if enabled
        if (SettingsManager.isAutoIndexEnabled(this)) {
            AutoIndexScheduler.schedule(
                this,
                SettingsManager.getAutoIndexHour(this),
                SettingsManager.getAutoIndexMinute(this)
            )
        }
    }
}
