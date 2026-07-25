package com.hlebushek.openscript

import android.app.Application
import com.hlebushek.openscript.notes.NotesRepository
import com.hlebushek.openscript.photos.DescriptionEngine
import com.hlebushek.openscript.photos.PhotoDescriptionIndexer
import com.hlebushek.openscript.photos.PhotoRepository
import com.google.android.material.color.DynamicColors

class App : Application() {
    val photos by lazy { PhotoRepository(this) }
    val descriptions by lazy { DescriptionEngine(this) }
    val notes by lazy { NotesRepository(this) }
    val photoIndexer by lazy { PhotoDescriptionIndexer(photos, descriptions) }

    override fun onCreate() {
        super.onCreate()
        // Material You: use wallpaper-based colors on Android 12+ when available.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Restore auto-index scheduling (no-op / cancel when disabled).
        AutoIndexScheduler.schedule(this)
    }
}
