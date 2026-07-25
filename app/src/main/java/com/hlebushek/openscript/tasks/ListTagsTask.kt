package com.hlebushek.openscript.tasks

import android.content.Context
import com.hlebushek.openscript.LanguageManager
import com.hlebushek.openscript.photos.PhotoRepository

class ListTagsTask(private val photoRepository: PhotoRepository) : TaskHandler {
    override val name = "List tags"
    override val example = "list all tags"
    override val exampleRu = "покажи все теги"

    private val listPatterns = listOf(
        Regex("(?i)list\\s+all\\s+tags"),
        Regex("(?i)show\\s+all\\s+tags"),
        Regex("(?i)all\\s+tags"),
        Regex("(?i)list\\s+tags"),
        Regex("(?i)покажи\\s+все\\s+теги"),
        Regex("(?i)список\\s+всех\\s+тегов"),
        Regex("(?i)все\\s+теги")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        // "list tags for this photo" belongs to ListPhotoTagsTask.
        if (normalized.contains("for this photo") || normalized.contains("для этого фото")) {
            return false
        }
        return listPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val language = LanguageManager.getLanguage(context)
        val tags = photoRepository.getAllTags()

        if (tags.isEmpty()) {
            val message = if (language == "ru") {
                "Теги не найдены. Сначала выполните: ${IndexPhotosTask().exampleRu}"
            } else {
                "No tags yet. Run this first: ${IndexPhotosTask().example}"
            }
            return TaskResult(message)
        }

        val items = tags.map { tag ->
            ResultItem(
                title = tag,
                subtitle = if (language == "ru") {
                    "Показать фото с этим тегом"
                } else {
                    "Show photos with this tag"
                },
                command = if (language == "ru") {
                    "найти фото по тегу $tag"
                } else {
                    "search photos by tag $tag"
                }
            )
        }

        val message = if (language == "ru") {
            "Найдено тегов: ${tags.size}"
        } else {
            "Found ${tags.size} tag(s)"
        }

        return TaskResult(message, items = items)
    }
}
