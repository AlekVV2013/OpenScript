package com.example.freeassistant.tasks

import android.content.Context
import com.example.freeassistant.LanguageManager

class ImportNotesTask : TaskHandler {
    override val name = "Import notes"
    override val example = "import notes"
    override val exampleRu = "импортировать заметки"

    private val importPatterns = listOf(
        Regex("(?i)import\\s+notes"),
        Regex("(?i)add\\s+notes"),
        Regex("(?i)load\\s+notes"),
        Regex("(?i)импортировать\\s+заметки"),
        Regex("(?i)добавить\\s+заметки"),
        Regex("(?i)загрузить\\s+заметки")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        return importPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val language = LanguageManager.getLanguage(context)
        val message = if (language == "ru") {
            "Выберите файл с заметками для импорта"
        } else {
            "Please select a notes file to import"
        }
        return TaskResult(message, action = TaskAction.PickNotesFolder)
    }
}
