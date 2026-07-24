package com.example.freeassistant.tasks

object IntentTranslator {
    fun toCanonical(input: String, language: String): String? {
        TimePhrases.findCommand(input, language)?.let { return it }
        WeatherPhrases.findCommand(input, language)?.let { return it }
        PhraseCatalog.findCommand(input, language)?.let { return it }
        
        return if (language == "ru") {
            RussianIntentMapper.map(input)
        } else {
            null
        }
    }
}
