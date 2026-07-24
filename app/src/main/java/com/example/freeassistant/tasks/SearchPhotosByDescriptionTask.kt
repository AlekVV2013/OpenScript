package com.example.freeassistant.tasks

import android.content.Context
import android.net.Uri
import com.example.freeassistant.LanguageManager
import com.example.freeassistant.photos.DescriptionEngine
import com.example.freeassistant.photos.PhotoRepository

class SearchPhotosByDescriptionTask(
    private val descriptionEngine: DescriptionEngine,
    private val photoRepository: PhotoRepository
) : TaskHandler {
    override val name = "Search photos by description"
    override val example = "search photos by description for cat"
    override val exampleRu = "найти фото по описанию кошка"

    private val searchPatterns = listOf(
        Regex("(?i)search\\s+photos?\\s+by\\s+description"),
        Regex("(?i)search\\s+photos?\\s+by\\s+tag"),
        Regex("(?i)find\\s+photos?\\s+by\\s+description"),
        Regex("(?i)find\\s+photos?\\s+by\\s+tag"),
        Regex("(?i)найти\\s+фото\\s+по\\s+описанию"),
        Regex("(?i)искать\\s+фото\\s+по\\s+описанию"),
        Regex("(?i)найти\\s+фото\\s+по\\s+тегу"),
        Regex("(?i)искать\\s+фото\\s+по\\s+тегу")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        if (normalized.startsWith("search photos by description") || 
            normalized.startsWith("search photos by tag") ||
            normalized.startsWith("find photos by description") ||
            normalized.startsWith("найти фото по описанию") ||
            normalized.startsWith("искать фото по описанию")) {
            return true
        }
        return searchPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val query = extractQuery(input)
        val allPhotos = photoRepository.getAllPhotos()
        val matchingPhotos = mutableListOf<Uri>()
        
        for (uri in allPhotos) {
            val description = descriptionEngine.getDescription(uri)
            if (description.contains(query, ignoreCase = true)) {
                matchingPhotos.add(uri)
            }
        }
        
        val language = LanguageManager.getLanguage(context)

        if (matchingPhotos.isEmpty()) {
            val message = if (language == "ru") {
                "Фото по описанию '$query' не найдены"
            } else {
                "No photos found with description '$query'"
            }
            return TaskResult(message)
        }

        val items = matchingPhotos.map { uri ->
            val name = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                .takeIf { it.isNotEmpty() } ?: "Photo"
            ResultItem(
                title = name,
                subtitle = "Matched by description",
                uri = uri
            )
        }

        val message = if (language == "ru") {
            "Найдено ${matchingPhotos.size} фото по описанию"
        } else {
            "Found ${matchingPhotos.size} photos by description"
        }

        return TaskResult(message, items = items)
    }

    private fun extractQuery(input: String): String {
        val lower = input.lowercase()
        val prefixes = listOf(
            "search photos by description for ", "search photos by description ",
            "search photos by tag for ", "search photos by tag ",
            "find photos by description for ", "find photos by description ",
            "find photos by tag for ", "find photos by tag ",
            "найти фото по описанию ", "искать фото по описанию ",
            "найти фото по тегу ", "искать фото по тегу "
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
