package com.example.freeassistant.tasks

import android.content.Context
import com.example.freeassistant.LanguageManager
import com.example.freeassistant.photos.PhotoRepository

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
        if (normalized.startsWith("search photos by name") || 
            normalized.startsWith("find photos by name") ||
            normalized.startsWith("найти фото по имени") ||
            normalized.startsWith("искать фото по имени")) {
            return true
        }
        return searchPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val query = extractQuery(input)
        val photos = photoRepository.searchByName(query)
        val language = LanguageManager.getLanguage(context)

        if (photos.isEmpty()) {
            val message = if (language == "ru") {
                "Фото по запросу '$query' не найдены"
            } else {
                "No photos found for '$query'"
            }
            return TaskResult(message)
        }

        val items = photos.map { uri ->
            val name = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                .takeIf { it.isNotEmpty() } ?: "Photo"
            ResultItem(
                title = name,
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
        val lower = input.lowercase()
        val prefixes = listOf(
            "search photos by name for ", "search photos by name ",
            "find photos by name for ", "find photos by name ",
            "search photos by filename for ", "search photos by filename ",
            "найти фото по имени ", "искать фото по имени ",
            "поиск фото по имени "
        )
        
        var query = input
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                query = input.substring(prefix.length)
                break
            }
        }
        return query.trim()
    }
}
