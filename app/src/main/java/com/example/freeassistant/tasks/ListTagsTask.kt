package com.example.freeassistant.tasks

import android.content.Context
import com.example.freeassistant.LanguageManager
import com.example.freeassistant.photos.DescriptionEngine

class ListTagsTask(private val descriptionEngine: DescriptionEngine) : TaskHandler {
    override val name = "List tags"
    override val example = "list all tags"
    override val exampleRu = "покажи все теги"

    private val listPatterns = listOf(
        Regex("(?i)list\\s+all\\s+tags"),
        Regex("(?i)show\\s+all\\s+tags"),
        Regex("(?i)all\\s+tags"),
        Regex("(?i)покажи\\s+все\\s+теги"),
        Regex("(?i)список\\s+всех\\s+тегов"),
        Regex("(?i)все\\s+теги")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        return listPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val language = LanguageManager.getLanguage(context)
        val message = if (language == "ru") {
            "Найденные теги:"
        } else {
            "Found tags:"
        }
        return TaskResult(message)
    }
}
