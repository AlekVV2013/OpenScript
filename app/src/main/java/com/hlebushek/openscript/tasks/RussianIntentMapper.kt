package com.hlebushek.openscript.tasks

object RussianIntentMapper {

    private val commandMap = mapOf(
        "помощь" to "help",
        "помоги" to "help",
        "что ты умеешь" to "help",
        "открыть приложение" to "open",
        "открой приложение" to "open",
        "запусти приложение" to "open",
        "индексировать фото" to "index my photos",
        "индексировать мои фото" to "index my photos",
        "сканировать фото" to "index my photos",
        "импортировать заметки" to "import notes",
        "искать фото" to "search photos",
        "найти фото" to "search photos",
        "искать заметки" to "search notes",
        "найти заметки" to "search notes",
        "какая погода" to "what is the weather",
        "погода" to "what is the weather",
        "который час" to "what time is it",
        "сколько времени" to "what time is it",
        "поставь будильник" to "set alarm",
        "установи будильник" to "set alarm",
        "поставь таймер" to "set timer",
        "установи таймер" to "set timer"
    )

    fun map(input: String): String? {
        val normalized = InputNormalizer.normalize(input)
        return commandMap[normalized]
    }
}
