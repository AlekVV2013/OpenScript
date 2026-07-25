package com.hlebushek.openscript

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "LanguagePrefs"
    private const val KEY_LANGUAGE = "language"
    private const val DEFAULT_LANGUAGE = "en"

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun applyLanguage(context: Context): Configuration {
        val language = getLanguage(context)
        val config = context.resources.configuration
        val locale = when (language) {
            "ru" -> Locale("ru")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        config.setLocale(locale)
        return config
    }
}
