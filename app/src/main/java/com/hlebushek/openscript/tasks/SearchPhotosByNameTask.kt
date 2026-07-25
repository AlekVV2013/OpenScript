package com.hlebushek.openscript.tasks

import android.content.Context
import com.hlebushek.openscript.LanguageManager
import com.hlebushek.openscript.photos.PhotoRepository

class SearchPhotosByNameTask(private val photoRepository: PhotoRepository) : TaskHandler {
    override val name = "Search photos by name"
    override val example = "search photos by name for cat"
    override val exampleRu = "найти фото по имени кошка"

    private val searchPatterns = listOf(
        Regex("(?i)search\\s+photos?\\s+by\\s+name"),
        Regex("(?i)find\\s+photos?\\s+by\\s+name"),
        Regex("(?i)search\\s+photos?\\s+by\\s+filename"),
        Regex("(?i)find\\s+photos?\\s+by\\s+filename"),
        Regex("(?i)найти\\s+фото\\s+по\\s+имени"),
        Regex("(?i)искать\\s+фото\\s+по\\s+имени"),
        Regex("(?i)поиск\\s+фото\\s+по\\s+имени")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        return searchPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val language = LanguageManager.getLanguage(context)
        val query = extractQuery(input)

        if (query.isBlank()) {
            val message = if (language == "ru") {
                "Скажите, что искать. Пример: $exampleRu"
            } else {
                "Tell me what to look for. Example: $example"
            }
            return TaskResult(message)
        }

        if (photoRepository.getAllPhotos().isEmpty()) {
            val message = if (language == "ru") {
                "Фото не проиндексированы. Сначала выполните: ${IndexPhotosTask().exampleRu}"
            } else {
                "No indexed photos. Run this first: ${IndexPhotosTask().example}"
            }
            return TaskResult(message)
        }

        val photos = photoRepository.searchByName(query)

        if (photos.isEmpty()) {
            val message = if (language == "ru") {
                "Фото по запросу '$query' не найдены"
            } else {
                "No photos found for '$query'"
            }
            return TaskResult(message)
        }

        val items = photos.map { uri ->
            ResultItem(
                title = photoRepository.getDisplayName(uri).ifBlank { "Photo" },
                subtitle = uri.toString(),
                uri = uri
            )
        }

        val message = if (language == "ru") {
            "Найдено ${photos.size} фото"
        } else {
            "Found ${photos.size} photos"
        }

        return TaskResult(message, items = items)
    }

    private fun extractQuery(input: String): String {
        val normalized = InputNormalizer.normalize(input)
        val prefixes = listOf(
            "search photos by name for ", "search photos by name ",
            "search photo by name for ", "search photo by name ",
            "find photos by name for ", "find photos by name ",
            "find photo by name for ", "find photo by name ",
            "search photos by filename for ", "search photos by filename ",
            "find photos by filename for ", "find photos by filename ",
            "найти фото по имени ", "искать фото по имени ",
            "поиск фото по имени "
        )

        for (prefix in prefixes) {
            if (normalized.startsWith(prefix)) {
                return normalized.substring(prefix.length).trim()
            }
        }
        return ""
    }
}
