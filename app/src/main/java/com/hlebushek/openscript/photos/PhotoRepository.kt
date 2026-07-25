package com.hlebushek.openscript.photos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(private val context: Context) {
    private val photoUris = mutableListOf<Uri>()
    private val photoNames = mutableMapOf<String, String>()
    private val photoTags = mutableMapOf<String, Set<String>>()

    suspend fun indexAllPhotos() = withContext(Dispatchers.IO) {
        val uris = mutableListOf<Uri>()
        val names = mutableMapOf<String, String>()

        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                uris.add(uri)
                names[uri.toString()] = cursor.getString(nameColumn).orEmpty()
            }
        }

        // Publish the fresh listing only after the query succeeded, and keep any
        // tags that were already computed for photos that still exist.
        synchronized(this@PhotoRepository) {
            photoUris.clear()
            photoUris.addAll(uris)
            photoNames.clear()
            photoNames.putAll(names)
            photoTags.keys.retainAll(names.keys)
        }
    }

    fun getAllPhotos(): List<Uri> = synchronized(this) { photoUris.toList() }

    fun getDisplayName(uri: Uri): String = synchronized(this) {
        photoNames[uri.toString()].orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
    }

    fun searchByName(query: String): List<Uri> {
        val lowerQuery = query.lowercase()
        if (lowerQuery.isBlank()) return emptyList()
        return synchronized(this) {
            photoUris.filter { uri ->
                photoNames[uri.toString()].orEmpty().lowercase().contains(lowerQuery)
            }
        }
    }

    fun getTagsForPhoto(uri: Uri): Set<String> = synchronized(this) {
        photoTags[uri.toString()] ?: emptySet()
    }

    fun setTagsForPhoto(uri: Uri, tags: Set<String>) {
        synchronized(this) { photoTags[uri.toString()] = tags }
    }

    fun getAllTags(): Set<String> = synchronized(this) {
        photoTags.values.flatten().toSortedSet()
    }

    fun searchByTags(tags: Set<String>): List<Uri> {
        if (tags.isEmpty()) return emptyList()
        return synchronized(this) {
            photoUris.filter { uri ->
                val tagsForPhoto = photoTags[uri.toString()] ?: emptySet()
                tags.any { tag -> tagsForPhoto.contains(tag) }
            }
        }
    }

    fun hasImagePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 34 -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
