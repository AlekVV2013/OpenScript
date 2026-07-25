package com.hlebushek.openscript.tasks

import android.content.Context
import com.hlebushek.openscript.LanguageManager
import com.hlebushek.openscript.weather.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class WeatherTask : TaskHandler {
    override val name = "Weather"
    override val example = "check the weather in London"
    override val exampleRu = "проверь погоду в London"

    private val patterns = listOf(
        Regex("(?i)^(?:check|show|get|tell me|what is|what's|how is|how's)?\\s*(?:the\\s+)?weather\\s+(?:in|for|at)\\s+(.+)$"),
        Regex("(?i)^weather\\s+(.+)$"),
        Regex("(?i)(?:покажи|показать|проверь|проверить|узнай|узнать|какая|что|прогноз|скажи|как)?\\s*погод[а-яё]*\\s+(?:в|для|у)\\s+(.+)"),
        Regex("(?i)погод[а-яё]*\\s+(?:в|для|у)\\s+(.+)")
    )

    override fun canHandle(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.equals("weather", true)) return true
        if (trimmed.equals("погода", true)) return true
        return firstGroup(input, patterns) != null
    }

    override suspend fun handle(input: String, context: Context): TaskResult =
        withContext(Dispatchers.IO) {
            val language = LanguageManager.getLanguage(context)
            val city = firstGroup(input, patterns)?.trim()

            if (city.isNullOrBlank()) {
                return@withContext if (language == "ru") {
                    TaskResult("Скажите город. Пример: $exampleRu")
                } else {
                    TaskResult("Tell me the city. Example: $example")
                }
            }

            val result = WeatherRepository.getWeather(
                context = context,
                city = city,
                language = language
            )

            result.fold(
                onSuccess = { weather ->
                    val temperature = String.format(
                        Locale.US,
                        "%.1f",
                        weather.temperatureC
                    )
                    val wind = String.format(
                        Locale.US,
                        "%.1f",
                        weather.windKmh
                    )
                    val windLabel = if (language == "ru") {
                        "ветер"
                    } else {
                        "wind"
                    }
                    val message = "${weather.city}: $temperature°C, ${weather.condition}, $windLabel $wind km/h"
                    TaskResult(message)
                },
                onFailure = { error ->
                    TaskResult(
                        "Weather failed: ${error.message ?: error.toString()}"
                    )
                }
            )
        }

    private fun firstGroup(input: String, patterns: List<Regex>): String? {
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(input)?.groups?.get(1)?.value
        }
    }
}
