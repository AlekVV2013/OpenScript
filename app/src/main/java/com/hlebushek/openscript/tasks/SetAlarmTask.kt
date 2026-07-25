package com.hlebushek.openscript.tasks

import android.content.Context
import com.hlebushek.openscript.LanguageManager
import java.util.Locale

class SetAlarmTask : TaskHandler {
    override val name = "Set alarm"
    override val example = "set alarm for 07:30"
    override val exampleRu = "поставь будильник на 07:30"

    private val settingWords = setOf(
        "set",
        "create",
        "add",
        "make",
        "schedule",
        "start",
        "wake",
        "поставь",
        "установи",
        "создай",
        "добавь",
        "запланируй",
        "сделай",
        "разбуди"
    )

    override fun canHandle(input: String): Boolean {
        val lower = input.lowercase().replace('ё', 'е')
        val alarmWord = lower.contains("alarm") ||
                lower.contains("будильник") ||
                lower.contains("разбуди")

        if (!alarmWord) return false

        val trimmed = lower.trim()
        if (trimmed == "alarm" || trimmed == "будильник") return true

        val hasSettingWord = settingWords.any { lower.contains(it) }
        return hasSettingWord || TimeParser.parseAlarm(input) != null
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val alarm = TimeParser.parseAlarm(input)

        if (alarm == null) {
            val language = LanguageManager.getLanguage(context)
            return if (language == "ru") {
                TaskResult("Скажите время будильника. Пример: $exampleRu")
            } else {
                TaskResult("Tell me the alarm time. Example: $example")
            }
        }

        val language = LanguageManager.getLanguage(context)
        val timeText = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
        val message = if (language == "ru") {
            "Будильник установлен на $timeText"
        } else {
            "Alarm set for $timeText"
        }

        val label = if (language == "ru") {
            "Будильник"
        } else {
            "Alarm"
        }

        return TaskResult(
            message = message,
            action = TaskAction.SetAlarm(
                hour = alarm.hour,
                minute = alarm.minute,
                label = label
            )
        )
    }
}
