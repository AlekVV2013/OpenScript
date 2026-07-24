package com.example.freeassistant.photos

import android.content.Context
import android.graphics.Bitmap
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

    suspend fun getDescription(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                Bitmap.createScaledBitmap(
                    android.graphics.BitmapFactory.decodeStream(it),
                    640,
                    480,
                    true
                )
            } ?: return@withContext ""

            val image = InputImage.fromBitmap(bitmap, 0)
            val labels = labeler.process(image).await()
            
            labels.joinToString(", ") { label ->
                label.text
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun indexPhoto(uri: Uri): Set<String> = withContext(Dispatchers.IO) {
        val description = getDescription(uri)
        if (description.isBlank()) {
            return@withContext emptySet()
        }
        description.split(",").map { it.trim().lowercase() }.toSet()
    }
}
