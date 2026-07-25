package com.hlebushek.openscript.tasks

object WeatherPhrases {

    private val weatherPhrases = mapOf(
        "en" to setOf(
            "what is the weather",
            "what's the weather",
            "whats the weather",
            "weather",
            "weather today",
            "current weather",
            "tell me the weather",
            "show me the weather",
            "weather forecast",
            "how is the weather",
            "what is the weather like",
            "is it raining"
        ),
        "ru" to setOf(
            "какая погода",
            "погода",
            "погода сегодня",
            "текущая погода",
            "скажи погоду",
            "покажи погоду",
            "прогноз погоды",
            "какая сейчас погода",
            "какая погода сегодня",
            "идет дождь",
            "какая погода на улице",
            "не идет ли дождь"
        )
    )

    fun findCommand(input: String, language: String): String? {
        val normalized = InputNormalizer.normalize(input)

        val matched = weatherPhrases[language]?.any {
            normalized == it || normalized.contains(it)
        } == true

        if (matched) {
            return if (language == "ru") "какая погода" else "what is the weather"
        }

        return null
    }
}
