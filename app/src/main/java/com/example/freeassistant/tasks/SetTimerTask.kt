package com.example.freeassistant.tasks

import android.content.Context
import com.example.freeassistant.LanguageManager

class SetTimerTask : TaskHandler {
    override val name = "Set timer"
    override val example = "set timer for 10 minutes"
    override val exampleRu = "поставь таймер на 10 минут"

    private val settingWords = setOf(
        "set",
        "start",
        "create",
        "make",
        "add",
        "schedule",
        "поставь",
        "запусти",
        "установи",
        "создай",
        "сделай",
        "добавь"
    )

    override fun canHandle(input: String): Boolean {
        val lower = input.lowercase().replace('ё', 'е')
        val timerWord = lower.contains("timer") ||
                lower.contains("countdown") ||
                lower.contains("таймер") ||
                lower.contains("обратный отсчет")

        if (!timerWord) return false

        val trimmed = lower.trim()
        if (trimmed == "timer" || trimmed == "таймер") return true

        val hasSettingWord = settingWords.any { lower.contains(it) }
        return hasSettingWord || TimeParser.parseTimerSeconds(input) != null
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val seconds = TimeParser.parseTimerSeconds(input)
        val language = LanguageManager.getLanguage(context)

        if (seconds == null || seconds <= 0) {
            return if (language == "ru") {
                TaskResult("Скажите длительность таймера. Пример: $exampleRu")
            } else {
                TaskResult("Tell me the timer duration. Example: $example")
            }
        }

        val durationText = TimeParser.formatDuration(seconds, language)
        val message = if (language == "ru") {
            "Таймер установлен: $durationText"
        } else {
            "Timer set: $durationText"
        }

        val label = if (language == "ru") {
            "Таймер"
        } else {
            "Timer"
        }

        return TaskResult(
            message = message,
            action = TaskAction.SetTimer(
                seconds = seconds,
                label = label
            )
        )
    }
}
