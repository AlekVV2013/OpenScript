package com.example.freeassistant.tasks

object InputNormalizer {
    fun normalize(input: String): String {
        return input.lowercase()
            .replace('\u0451', '\u0435') // ё -> е
            .replace(Regex("[?!.,;\"\"'()\[\]{}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun normalizeForMatching(input: String): String {
        return normalize(input)
    }
}
