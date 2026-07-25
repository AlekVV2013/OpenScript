package com.hlebushek.openscript.tasks

object RegexUtils {
    fun createPattern(vararg parts: String, ignoreCase: Boolean = true): Regex {
        val pattern = parts.joinToString("|")
        return if (ignoreCase) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
    }

    fun matchAny(input: String, patterns: List<Regex>): Boolean {
        return patterns.any { it.containsMatchIn(input) }
    }

    fun extractFirstMatch(input: String, pattern: Regex): String? {
        return pattern.find(input)?.groupValues?.get(1)
    }

    fun extractAllMatches(input: String, pattern: Regex): List<String> {
        return pattern.findAll(input).map { it.groupValues[1] }.toList()
    }
}
