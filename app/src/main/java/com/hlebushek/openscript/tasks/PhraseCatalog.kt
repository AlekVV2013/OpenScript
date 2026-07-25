package com.hlebushek.openscript.tasks

object PhraseCatalog {

    private val generalPhrases = mapOf(
        "en" to setOf(
            "hello",
            "hi",
            "hey",
            "good morning",
            "good afternoon",
            "good evening",
            "how are you",
            "what can you do",
            "help",
            "help me",
            "what can you do for me",
            "show me what you can do"
        ),
        "ru" to setOf(
            "привет",
            "здравствуйте",
            "доброе утро",
            "добрый день",
            "добрый вечер",
            "как дела",
            "что ты умеешь",
            "помощь",
            "помоги",
            "помогите",
            "что ты можешь",
            "покажи что ты умеешь"
        )
    )

    private val openAppPhrases = mapOf(
        "en" to setOf(
            "open",
            "launch",
            "start",
            "run",
            "open app",
            "launch app",
            "start app",
            "run app",
            "open application",
            "launch application"
        ),
        "ru" to setOf(
            "открой",
            "запусти",
            "открыть",
            "запустить",
            "открой приложение",
            "запусти приложение",
            "открыть приложение",
            "запустить приложение"
        )
    )

    private val photoPhrases = mapOf(
        "en" to setOf(
            "index my photos",
            "index photos",
            "scan my photos",
            "scan photos",
            "tag my photos",
            "tag photos",
            "search photos",
            "find photos",
            "search photos by name",
            "find photos by name"
        ),
        "ru" to setOf(
            "индексировать мои фото",
            "индексировать фото",
            "сканировать мои фото",
            "сканировать фото",
            "тегировать мои фото",
            "тегировать фото",
            "искать фото",
            "найти фото",
            "искать фото по имени",
            "найти фото по имени"
        )
    )

    private val notesPhrases = mapOf(
        "en" to setOf(
            "import notes",
            "add notes",
            "load notes",
            "search notes",
            "find notes",
            "search in notes",
            "find in notes"
        ),
        "ru" to setOf(
            "импортировать заметки",
            "добавить заметки",
            "загрузить заметки",
            "искать заметки",
            "найти заметки",
            "искать в заметках",
            "найти в заметках"
        )
    )

    fun findCommand(input: String, language: String): String? {
        val normalized = InputNormalizer.normalize(input)

        // Most specific groups are checked first so that, for example,
        // "index my photos" is not swallowed by the generic "open"/"help" sets.

        // Check photo phrases
        if (matches(photoPhrases[language], normalized)) {
            return if (language == "ru") "индексировать мои фото" else "index my photos"
        }

        // Check notes phrases
        if (matches(notesPhrases[language], normalized)) {
            return if (language == "ru") "импортировать заметки" else "import notes"
        }

        // Check open app phrases
        if (matches(openAppPhrases[language], normalized)) {
            return if (language == "ru") "открыть приложение" else "open"
        }

        // Check general phrases
        if (matches(generalPhrases[language], normalized)) {
            return if (language == "ru") "помощь" else "help"
        }

        return null
    }

    private fun matches(phrases: Set<String>?, normalized: String): Boolean {
        return phrases?.any { normalized.contains(it) } == true
    }
}
