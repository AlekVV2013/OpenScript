package com.example.freeassistant

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentUtteranceId: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = when (LanguageManager.getLanguage(context)) {
                "ru" -> Locale("ru")
                else -> Locale("en")
            }
            isReady = true
        }
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        
        val gender = SettingsManager.getVoiceGender(context)
        val params = Bundle().apply {
            when (gender) {
                "male" -> putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, -1.0f)
                "female" -> putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 1.0f)
            }
        }
        
        currentUtteranceId = System.currentTimeMillis().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, currentUtteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }
}
