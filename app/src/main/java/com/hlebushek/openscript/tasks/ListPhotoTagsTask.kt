package com.hlebushek.openscript.tasks

import android.content.Context
import com.hlebushek.openscript.LanguageManager
import com.hlebushek.openscript.photos.PhotoRepository

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
        // There is no photo picker in the chat UI, so "this photo" resolves to the
        // most recently added photo from the last index run.
        val photo = photoRepository.getAllPhotos().firstOrNull()

        if (photo == null) {
            val message = if (language == "ru") {
                "Фото не найдены. Сначала выполните: ${IndexPhotosTask().exampleRu}"
            } else {
                "No photos found. Run this first: ${IndexPhotosTask().example}"
            }
            return TaskResult(message)
        }

        val displayName = photoRepository.getDisplayName(photo)
        val tags = photoRepository.getTagsForPhoto(photo)

        if (tags.isEmpty()) {
            val message = if (language == "ru") {
                "У фото \"$displayName\" нет тегов"
            } else {
                "No tags for \"$displayName\""
            }
            return TaskResult(
                message,
                items = listOf(ResultItem(title = displayName, uri = photo))
            )
        }

        val message = if (language == "ru") {
            "Теги для \"$displayName\": ${tags.joinToString(", ")}"
        } else {
            "Tags for \"$displayName\": ${tags.joinToString(", ")}"
        }

        return TaskResult(
            message,
            items = listOf(
                ResultItem(
                    title = displayName,
                    subtitle = tags.joinToString(", "),
                    uri = photo
                )
            )
        )
    }
}
