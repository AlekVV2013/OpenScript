package com.example.freeassistant.tasks

import android.content.Context
import com.example.freeassistant.LanguageManager

class IndexPhotosTask : TaskHandler {
    override val name = "Index photos"
    override val example = "index my photos"
    override val exampleRu = "индексировать мои фото"

    private val indexPatterns = listOf(
        Regex("(?i)index\\s+(my\\s+)?photos?"),
        Regex("(?i)scan\\s+(my\\s+)?photos?"),
        Regex("(?i)tag\\s+(my\\s+)?photos?"),
        Regex("(?i)индексировать\\s+(мои\\s+)?фото"),
        Regex("(?i)сканировать\\s+(мои\\s+)?фото"),
        Regex("(?i)тегировать\\s+(мои\\s+)?фото")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        return indexPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val language = LanguageManager.getLanguage(context)
        val message = if (language == "ru") {
            "Индексирование фото..."
        } else {
            "Indexing photos..."
        }
        return TaskResult(message, action = TaskAction.IndexPhotos)
    }
}
