package com.example.freeassistant.tasks

import android.content.Context
import com.example.freeassistant.LanguageManager
import com.example.freeassistant.weather.WeatherRepository

class WeatherTask(private val weatherRepository: WeatherRepository) : TaskHandler {
    override val name = "Weather"
    override val example = "what is the weather"
    override val exampleRu = "какая погода"

    private val weatherPatterns = listOf(
        Regex("(?i)^(what is the weather|what's the weather|whats the weather|weather|weather today|current weather|tell me the weather|show me the weather|weather forecast|how is the weather|what is the weather like|is it raining)$"),
        Regex("(?i)^(какая погода|погода|погода сегодня|текущая погода|скажи погоду|покажи погоду|прогноз погоды|какая сейчас погода|какая погода сегодня|идет дождь|какая погода на улице|не идет ли дождь)$")
    )

    override fun canHandle(input: String): Boolean {
        val trimmed = input.trim()
        return weatherPatterns.any { it.matches(trimmed) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val language = LanguageManager.getLanguage(context)
        val city = if (language == "ru") "Moscow" else "London"
        
        val weather = weatherRepository.getWeather(city)
        
        return TaskResult(weather)
    }
}
