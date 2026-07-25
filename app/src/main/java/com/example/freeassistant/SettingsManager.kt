package com.example.freeassistant

import android.content.Context
import java.util.Locale

object SettingsManager {
    private const val PREFS_NAME = "SettingsPrefs"
    private const val KEY_AUTO_INDEX = "auto_index"
    private const val KEY_AUTO_INDEX_TIME = "auto_index_time"
    private const val KEY_SPEECH_OUTPUT = "speech_output"
    private const val KEY_VOICE_GENDER = "voice_gender"
    private const val KEY_OPENWEATHER_API_KEY = "openweather_api_key"
    private const val DEFAULT_AUTO_INDEX_TIME = "02:00"

    fun isAutoIndexEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_INDEX, false)
    }

    fun setAutoIndexEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_INDEX, enabled).apply()
    }

    fun getAutoIndexTime(context: Context): String {
        return prefs(context).getString(KEY_AUTO_INDEX_TIME, DEFAULT_AUTO_INDEX_TIME) ?: DEFAULT_AUTO_INDEX_TIME
    }

    fun setAutoIndexTime(context: Context, time: String) {
        prefs(context).edit().putString(KEY_AUTO_INDEX_TIME, time).apply()
    }

    fun getAutoIndexHour(context: Context): Int {
        return getAutoIndexTimePart(context, index = 0, defaultValue = 2)
    }

    fun getAutoIndexMinute(context: Context): Int {
        return getAutoIndexTimePart(context, index = 1, defaultValue = 0)
    }

    fun setAutoIndexTime(context: Context, hour: Int, minute: Int) {
        val time = String.format(Locale.US, "%02d:%02d", hour, minute)
        setAutoIndexTime(context, time)
    }

    fun isSpeechOutputEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SPEECH_OUTPUT, true)
    }

    fun setSpeechOutputEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SPEECH_OUTPUT, enabled).apply()
    }

    fun getVoiceGender(context: Context): String {
        return prefs(context).getString(KEY_VOICE_GENDER, "female") ?: "female"
    }

    fun setVoiceGender(context: Context, gender: String) {
        prefs(context).edit().putString(KEY_VOICE_GENDER, gender).apply()
    }

    fun getVoice(context: Context): String {
        return getVoiceGender(context)
    }

    fun setVoice(context: Context, voice: String) {
        setVoiceGender(context, voice)
    }

    fun getOpenWeatherApiKey(context: Context): String {
        return prefs(context)
            .getString(KEY_OPENWEATHER_API_KEY, "")
            .orEmpty()
    }

    fun setOpenWeatherApiKey(context: Context, key: String) {
        prefs(context)
            .edit()
            .putString(KEY_OPENWEATHER_API_KEY, key)
            .apply()
    }

    private fun getAutoIndexTimePart(context: Context, index: Int, defaultValue: Int): Int {
        val parts = getAutoIndexTime(context).split(":")
        return parts.getOrNull(index)?.toIntOrNull() ?: defaultValue
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
