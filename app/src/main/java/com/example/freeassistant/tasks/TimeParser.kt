package com.example.freeassistant.tasks

import java.util.Calendar

object TimeParser {

    data class AlarmTime(
        val hour: Int,
        val minute: Int
    )

    private val hoursRegex = Regex(
        "(\\d+)\\s*(?:h|hr|hrs|hour|hours|ч|час|часа|часов)",
        RegexOption.IGNORE_CASE
    )

    private val minutesRegex = Regex(
        "(\\d+)\\s*(?:m|min|mins|minute|minutes|мин|минута|минуты|минут)",
        RegexOption.IGNORE_CASE
    )

    private val secondsRegex = Regex(
        "(\\d+)\\s*(?:s|sec|secs|second|seconds|сек|секунда|секунды|секунд)",
        RegexOption.IGNORE_CASE
    )

    private val timeRegex = Regex(
        "(\\d{1,2}):(\\d{2})\\s*(am|pm|утра|вечера|дня|ночи)?",
        RegexOption.IGNORE_CASE
    )

    private val hourRegex = Regex(
        "(?:^|\\s)(?:at|for|to|в|на)?\\s*(\\d{1,2})\\s*(am|pm|утра|вечера|дня|ночи)?(?:\\s|$)",
        RegexOption.IGNORE_CASE
    )

    private val colonDurationRegex = Regex(
        "(\\d+):(\\d{2})"
    )

    private val standaloneNumberRegex = Regex(
        "(?:^|\\s)(\\d+)(?:\\s|$)"
    )

    private val durationUnitRegex = Regex(
        "(?i)(\\bseconds?\\b|\\bsec\\b|\\bminutes?\\b|\\bmin\\b|\\bhours?\\b|\\bhr\\b|\\bh\\b|сек[а-яё]*|мин[а-яё]*|час[а-яё]*)"
    )

    private val relativeRegex = Regex(
        "(?i)(\\bin\\b|\\bafter\\b|через|после|спустя)"
    )

    fun parseAlarm(input: String): AlarmTime? {
        val text = normalize(input)

        if (text.isEmpty()) return null

        val durationSeconds = parseTimerSeconds(text)
        val hasDurationUnits = durationUnitRegex.containsMatchIn(text)
        val hasRelativeWord = relativeRegex.containsMatchIn(text)

        if ((hasDurationUnits || hasRelativeWord) && durationSeconds != null && durationSeconds > 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, durationSeconds)
            return AlarmTime(
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE)
            )
        }

        timeRegex.find(text)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            val meridiem = match.groupValues[3].takeIf { it.isNotBlank() }
            val adjustedHour = adjustHour(hour, meridiem)
            if (adjustedHour !in 0..23 || minute !in 0..59) {
                return null
            }
            return AlarmTime(adjustedHour, minute)
        }

        hourRegex.find(text)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val meridiem = match.groupValues[2].takeIf { it.isNotBlank() }
            val adjustedHour = adjustHour(hour, meridiem)
            if (adjustedHour !in 0..23) {
                return null
            }
            return AlarmTime(adjustedHour, 0)
        }

        return null
    }

    fun parseTimerSeconds(input: String): Int? {
        val text = normalize(input)

        if (text.isEmpty()) return null

        var seconds = 0
        hoursRegex.findAll(text).forEach { match ->
            val value = match.groupValues[1].toIntOrNull() ?: 0
            seconds += value * 3600
        }

        minutesRegex.findAll(text).forEach { match ->
            val value = match.groupValues[1].toIntOrNull() ?: 0
            seconds += value * 60
        }

        secondsRegex.findAll(text).forEach { match ->
            val value = match.groupValues[1].toIntOrNull() ?: 0
            seconds += value
        }

        if (seconds > 0) {
            return seconds
        }

        colonDurationRegex.find(text)?.let { match ->
            val minutes = match.groupValues[1].toIntOrNull() ?: return null
            val secs = match.groupValues[2].toIntOrNull() ?: return null
            if (secs in 0..59) {
                return minutes * 60 + secs
            }
        }

        standaloneNumberRegex.find(text)?.let { match ->
            val value = match.groupValues[1].toIntOrNull() ?: return null
            return value * 60
        }

        return null
    }

    fun formatDuration(totalSeconds: Int, language: String): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val parts = mutableListOf<String>()

        if (language == "ru") {
            if (hours > 0) parts.add("$hours ч")
            if (minutes > 0) parts.add("$minutes мин")
            if (seconds > 0 || parts.isEmpty()) parts.add("$seconds сек")
        } else {
            if (hours > 0) parts.add("$hours h")
            if (minutes > 0) parts.add("$minutes min")
            if (seconds > 0 || parts.isEmpty()) parts.add("$seconds sec")
        }

        return parts.joinToString(" ")
    }

    fun normalize(input: String): String {
        return input.lowercase()
            .replace('ё', 'е')
            .replace(Regex("""[?!.,;"'()\[\]{}]"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun adjustHour(hour: Int, meridiem: String?): Int {
        if (meridiem == null) return hour

        val m = meridiem.lowercase()

        return when {
            m.startsWith("a") || m == "утра" || m == "ночи" -> {
                if (hour == 12) 0 else hour
            }
            m.startsWith("p") || m == "дня" || m == "вечера" -> {
                if (hour < 12) hour + 12 else hour
            }
            else -> hour
        }
    }
}
