package com.hlebushek.openscript.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DescriptionEngine(private val context: Context) {
    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    /** Cache keyed by photo uri so repeated searches do not re-run ML Kit. */
    private val cache = mutableMapOf<String, List<String>>()

    suspend fun getDescription(uri: Uri): String = getLabels(uri).joinToString(", ")

    suspend fun indexPhoto(uri: Uri): Set<String> = getLabels(uri).toSet()

    fun getCachedTags(): Set<String> = synchronized(cache) {
        cache.values.flatten().toSortedSet()
    }

    fun clearCache() = synchronized(cache) { cache.clear() }

    private suspend fun getLabels(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val key = uri.toString()
        synchronized(cache) { cache[key] }?.let { return@withContext it }

        val labels = try {
            val bitmap = decodeScaled(uri)
            if (bitmap == null) {
                emptyList()
            } else {
                val image = InputImage.fromBitmap(bitmap, 0)
                labeler.process(image).await()
                    .map { label -> label.text.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .distinct()
            }
        } catch (_: Exception) {
            // Unreadable/corrupt image or ML Kit failure: treat as "no labels".
            emptyList()
        }

        synchronized(cache) { cache[key] = labels }
        labels
    }

    /**
     * Decodes the image downscaled to roughly [TARGET_SIZE] using inSampleSize so
     * that large photos never need to be fully loaded into memory.
     */
    private fun decodeScaled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= TARGET_SIZE &&
            bounds.outHeight / (sampleSize * 2) >= TARGET_SIZE
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private companion object {
        const val TARGET_SIZE = 480
    }
}
