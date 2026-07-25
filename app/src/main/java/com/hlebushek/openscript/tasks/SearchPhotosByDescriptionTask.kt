package com.hlebushek.openscript.tasks

import android.content.Context
import android.net.Uri
import com.hlebushek.openscript.LanguageManager
import com.hlebushek.openscript.photos.DescriptionEngine
import com.hlebushek.openscript.photos.PhotoRepository

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

        val allPhotos = photoRepository.getAllPhotos()
        if (allPhotos.isEmpty()) {
            val message = if (language == "ru") {
                "Фото не проиндексированы. Сначала выполните: ${IndexPhotosTask().exampleRu}"
            } else {
                "No indexed photos. Run this first: ${IndexPhotosTask().example}"
            }
            return TaskResult(message)
        }

        val matchingPhotos = mutableListOf<Uri>()
        for (uri in allPhotos) {
            // Prefer tags stored during indexing; fall back to labelling on demand
            // (DescriptionEngine caches results, so this happens at most once per photo).
            val tags = photoRepository.getTagsForPhoto(uri).ifEmpty {
                descriptionEngine.indexPhoto(uri).also { fresh ->
                    if (fresh.isNotEmpty()) photoRepository.setTagsForPhoto(uri, fresh)
                }
            }
            if (tags.any { it.contains(query, ignoreCase = true) }) {
                matchingPhotos.add(uri)
            }
        }

        if (matchingPhotos.isEmpty()) {
            val message = if (language == "ru") {
                "Фото по описанию '$query' не найдены"
            } else {
                "No photos found with description '$query'"
            }
            return TaskResult(message)
        }

        val items = matchingPhotos.map { uri ->
            ResultItem(
                title = photoRepository.getDisplayName(uri).ifBlank { "Photo" },
                subtitle = photoRepository.getTagsForPhoto(uri).joinToString(", "),
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
        val normalized = InputNormalizer.normalize(input)
        val prefixes = listOf(
            "search photos by description for ", "search photos by description ",
            "search photo by description for ", "search photo by description ",
            "search photos by tag for ", "search photos by tag ",
            "search photo by tag for ", "search photo by tag ",
            "find photos by description for ", "find photos by description ",
            "find photo by description for ", "find photo by description ",
            "find photos by tag for ", "find photos by tag ",
            "find photo by tag for ", "find photo by tag ",
            "найти фото по описанию ", "искать фото по описанию ",
            "найти фото по тегу ", "искать фото по тегу "
        )

        for (prefix in prefixes) {
            if (normalized.startsWith(prefix)) {
                return normalized.substring(prefix.length).trim()
            }
        }
        return ""
    }
}
