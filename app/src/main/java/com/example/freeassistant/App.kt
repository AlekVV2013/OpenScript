package com.example.freeassistant

import android.app.Application
import com.example.freeassistant.photos.PhotoRepository
import com.example.freeassistant.notes.NotesRepository
import com.example.freeassistant.weather.WeatherRepository
import com.example.freeassistant.photos.DescriptionEngine

class App : Application() {
    val photos by lazy { PhotoRepository(this) }
    val descriptions by lazy { DescriptionEngine(this) }
    val notes by lazy { NotesRepository(this) }
    val weather by lazy { WeatherRepository(this) }
}
