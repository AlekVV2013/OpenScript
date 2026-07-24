package com.example.freeassistant.tasks

import android.content.Context
import android.text.format.DateFormat
import com.example.freeassistant.LanguageManager
import java.util.Date

class CurrentTimeTask : TaskHandler {
    override val name = "Current time"
    override val example = "what time is it"
    override val exampleRu = "который час"

    private val patterns = listOf(
        Regex("(?i)^(?:what time is it|what's the time|whats the time|what is the time|current time(?: now)?|time now|time|tell me the(?: current)? time|show me the(?: current)? time|the time)$"),
        Regex("(?i)^(?:который час|сколько времени|текущее время|какое сейчас время|время сейчас|время|покажи(?: мне)?(?: текущее)? время|скажи(?: мне)?(?: текущее)? время|что за время сейчас)$")
    )

    override fun canHandle(input: String): Boolean {
        val trimmed = input.trim()
        return patterns.any { it.matches(trimmed) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val timeFormat = DateFormat.getTimeFormat(context)
        val currentTime = timeFormat.format(Date())

        val language = LanguageManager.getLanguage(context)
        val message = if (language == "ru") {
            "Текущее время: $currentTime"
        } else {
            "Current time: $currentTime"
        }

        return TaskResult(message)
    }
}
