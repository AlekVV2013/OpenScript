package com.example.freeassistant.photos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(private val context: Context) {
    private val photoUris = mutableListOf<Uri>()
    private val photoTags = mutableMapOf<String, Set<String>>()

    suspend fun indexAllPhotos() = withContext(Dispatchers.IO) {
        photoUris.clear()
        photoTags.clear()

        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                photoUris.add(uri)
            }
        }
    }

    fun getAllPhotos(): List<Uri> = photoUris.toList()

    fun searchByName(query: String): List<Uri> {
        val lowerQuery = query.lowercase()
        return photoUris.filter { uri ->
            val name = DocumentFile.fromSingleUri(context, uri)?.name?.lowercase() ?: ""
            name.contains(lowerQuery)
        }
    }

    fun getTagsForPhoto(uri: Uri): Set<String> {
        return photoTags[uri.toString()] ?: emptySet()
    }

    fun setTagsForPhoto(uri: Uri, tags: Set<String>) {
        photoTags[uri.toString()] = tags
    }

    fun getAllTags(): Set<String> {
        return photoTags.values.flatten().toSet()
    }

    fun searchByTags(tags: Set<String>): List<Uri> {
        return photoUris.filter { uri ->
            val photoTags = getTagsForPhoto(uri)
            tags.any { tag -> photoTags.contains(tag) }
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
