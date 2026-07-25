package com.hlebushek.openscript.tasks

import android.content.Context
import com.hlebushek.openscript.LanguageManager
import com.hlebushek.openscript.notes.NotesRepository

class SearchNotesTask(private val notesRepository: NotesRepository) : TaskHandler {
    override val name = "Search notes"
    override val example = "search notes for meeting"
    override val exampleRu = "найти заметки про встречу"

    private val searchPatterns = listOf(
        Regex("(?i)search\\s+notes\\s+for"),
        Regex("(?i)search\\s+notes\\s+about"),
        Regex("(?i)find\\s+notes\\s+for"),
        Regex("(?i)find\\s+notes\\s+about"),
        Regex("(?i)search\\s+in\\s+notes\\s+for"),
        Regex("(?i)search\\s+in\\s+notes"),
        Regex("(?i)искать\\s+заметки\\s+про"),
        Regex("(?i)найти\\s+заметки\\s+про"),
        Regex("(?i)искать\\s+в\\s+заметках"),
        Regex("(?i)поиск\\s+в\\s+заметках")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        if (normalized.startsWith("search notes") || normalized.startsWith("find notes") ||
            normalized.startsWith("search in notes") ||
            normalized.startsWith("искать заметки") || normalized.startsWith("найти заметки") ||
            normalized.startsWith("искать в заметках") || normalized.startsWith("поиск в заметках")) {
            return true
        }
        return searchPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val query = extractQuery(input)
        val notes = notesRepository.search(query)
        val language = LanguageManager.getLanguage(context)
        
        if (notes.isEmpty()) {
            val message = if (language == "ru") {
                "Заметки по запросу '$query' не найдены"
            } else {
                "No notes found for '$query'"
            }
            return TaskResult(message)
        }

        val items = notes.map { note ->
            ResultItem(
                title = note.title,
                subtitle = note.content.take(100) + if (note.content.length > 100) "..." else "",
                text = note.content
            )
        }

        val message = if (language == "ru") {
            "Найдено ${notes.size} заметок"
        } else {
            "Found ${notes.size} notes"
        }

        return TaskResult(message, items = items)
    }

    private fun extractQuery(input: String): String {
        val lower = input.lowercase()
        val prefixes = listOf(
            "search notes for ", "search notes about ", "find notes for ",
            "find notes about ", "search in notes for ", "search in notes ",
            "искать заметки про ", "найти заметки про ", "искать в заметках ",
            "поиск в заметках ", "search notes ", "find notes ",
            "искать заметки ", "найти заметки "
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
