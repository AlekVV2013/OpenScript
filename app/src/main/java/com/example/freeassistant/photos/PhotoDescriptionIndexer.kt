package com.example.freeassistant.photos

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoDescriptionIndexer(
    private val context: Context,
    private val photoRepository: PhotoRepository,
    private val descriptionEngine: DescriptionEngine
) {
    suspend fun indexAllPhotos() = withContext(Dispatchers.IO) {
        val photos = photoRepository.getAllPhotos()
        photos.forEach { uri ->
            val tags = descriptionEngine.indexPhoto(uri)
            photoRepository.setTagsForPhoto(uri, tags)
        }
    }
}
