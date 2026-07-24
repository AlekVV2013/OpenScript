package com.example.freeassistant

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "SettingsPrefs"
    private const val KEY_AUTO_INDEX = "auto_index"
    private const val KEY_AUTO_INDEX_TIME = "auto_index_time"
    private const val KEY_SPEECH_OUTPUT = "speech_output"
    private const val KEY_VOICE_GENDER = "voice_gender"
    private const val DEFAULT_AUTO_INDEX_TIME = "02:00"

    fun isAutoIndexEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_INDEX, false)
    }

    fun setAutoIndexEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_INDEX, enabled).apply()
    }

    fun getAutoIndexTime(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTO_INDEX_TIME, DEFAULT_AUTO_INDEX_TIME) ?: DEFAULT_AUTO_INDEX_TIME
    }

    fun setAutoIndexTime(context: Context, time: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTO_INDEX_TIME, time).apply()
    }

    fun isSpeechOutputEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SPEECH_OUTPUT, true)
    }

    fun setSpeechOutputEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SPEECH_OUTPUT, enabled).apply()
    }

    fun getVoiceGender(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VOICE_GENDER, "female") ?: "female"
    }

    fun setVoiceGender(context: Context, gender: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VOICE_GENDER, gender).apply()
    }
}
