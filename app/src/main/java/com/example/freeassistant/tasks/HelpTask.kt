package com.example.freeassistant.tasks

import android.content.Context
import com.example.freeassistant.LanguageManager

class HelpTask(private val allHandlers: () -> List<TaskHandler>) : TaskHandler {
    override val name = "Help"
    override val example = "help"
    override val exampleRu = "помощь"

    private val helpPatterns = listOf(
        Regex("(?i)^(help|what can you do|what can i do|commands|show commands|list commands)$"),
        Regex("(?i)^(помощь|что ты умеешь|что я могу|команды|покажи команды|список команд)$")
    )

    override fun canHandle(input: String): Boolean {
        val trimmed = input.trim()
        return helpPatterns.any { it.matches(trimmed) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val handlers = allHandlers()
        val language = LanguageManager.getLanguage(context)

        val examples = handlers.map { handler ->
            if (language == "ru") handler.exampleRu else handler.example
        }

        val message = if (language == "ru") {
            "Достupные команды:\n" + examples.joinToString("\n")
        } else {
            "Available commands:\n" + examples.joinToString("\n")
        }

        return TaskResult(message)
    }
}
