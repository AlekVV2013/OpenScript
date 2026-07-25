package com.example.freeassistant

import android.app.Application
import com.example.freeassistant.notes.NotesRepository
import com.example.freeassistant.photos.DescriptionEngine
import com.example.freeassistant.photos.PhotoRepository

class App : Application() {
    val photos by lazy { PhotoRepository(this) }
    val descriptions by lazy { DescriptionEngine(this) }
    val notes by lazy { NotesRepository(this) }
}
