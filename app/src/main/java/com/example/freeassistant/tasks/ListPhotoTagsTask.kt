package com.example.freeassistant.tasks

import android.content.Context
import android.net.Uri
import com.example.freeassistant.LanguageManager
import com.example.freeassistant.photos.PhotoRepository

class ListPhotoTagsTask(private val photoRepository: PhotoRepository) : TaskHandler {
    override val name = "List photo tags"
    override val example = "list tags for this photo"
    override val exampleRu = "покажи теги для этого фото"

    private val listPatterns = listOf(
        Regex("(?i)list\\s+tags\\s+for\\s+this\\s+photo"),
        Regex("(?i)show\\s+tags\\s+for\\s+this\\s+photo"),
        Regex("(?i)tags\\s+for\\s+this\\s+photo"),
        Regex("(?i)покажи\\s+теги\\s+для\\s+этого\\s+фото"),
        Regex("(?i)список\\s+тегов\\s+для\\s+этого\\s+фото"),
        Regex("(?i)теги\\s+для\\s+этого\\s+фото")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        return listPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val language = LanguageManager.getLanguage(context)
        val message = if (language == "ru") {
            "Теги для выбранного фото"
        } else {
            "Tags for selected photo"
        }
        return TaskResult(message)
    }
}
