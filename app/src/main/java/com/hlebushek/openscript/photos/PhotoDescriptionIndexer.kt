package com.hlebushek.openscript.photos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Rebuilds the MediaStore listing and then computes on-device ML Kit labels for
 * every photo, storing them on [PhotoRepository] so tag queries have data.
 */
class PhotoDescriptionIndexer(
    private val photoRepository: PhotoRepository,
    private val descriptionEngine: DescriptionEngine
) {
    suspend fun indexAllPhotos(): Int = withContext(Dispatchers.IO) {
        photoRepository.indexAllPhotos()
        val photos = photoRepository.getAllPhotos()
        photos.forEach { uri ->
            val tags = descriptionEngine.indexPhoto(uri)
            photoRepository.setTagsForPhoto(uri, tags)
        }
        photos.size
    }
}
