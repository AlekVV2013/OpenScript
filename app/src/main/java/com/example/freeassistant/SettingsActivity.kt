package com.example.freeassistant

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : BaseActivity() {
    private lateinit var languageRadioGroup: RadioGroup
    private lateinit var autoIndexSwitch: SwitchMaterial
    private lateinit var autoIndexTimeButton: Button
    private lateinit var speechOutputSwitch: SwitchMaterial
    private lateinit var voiceRadioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize views
        languageRadioGroup = findViewById(R.id.languageRadioGroup)
        autoIndexSwitch = findViewById(R.id.autoIndexSwitch)
        autoIndexTimeButton = findViewById(R.id.autoIndexTimeButton)
        speechOutputSwitch = findViewById(R.id.speechOutputSwitch)
        voiceRadioGroup = findViewById(R.id.voiceRadioGroup)

        // Load settings
        loadSettings()

        // Setup listeners
        languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val language = when (checkedId) {
                R.id.radioEnglish -> "en"
                R.id.radioRussian -> "ru"
                else -> "en"
            }
            LanguageManager.setLanguage(this, language)
            recreate()
        }

        autoIndexSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setAutoIndexEnabled(this, isChecked)
            if (isChecked) {
                val time = SettingsManager.getAutoIndexTime(this)
                val parts = time.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: 2
                    val minute = parts[1].toIntOrNull() ?: 0
                    AutoIndexScheduler.schedule(this, hour, minute)
                }
            } else {
                AutoIndexScheduler.cancel(this)
            }
        }

        autoIndexTimeButton.setOnClickListener {
            showTimePicker()
        }

        speechOutputSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setSpeechOutputEnabled(this, isChecked)
        }

        voiceRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val gender = when (checkedId) {
                R.id.radioVoiceFemale -> "female"
                R.id.radioVoiceMale -> "male"
                else -> "female"
            }
            SettingsManager.setVoiceGender(this, gender)
        }
    }

    private fun loadSettings() {
        // Language
        val language = LanguageManager.getLanguage(this)
        val languageRadioId = when (language) {
            "ru" -> R.id.radioRussian
            else -> R.id.radioEnglish
        }
        languageRadioGroup.check(languageRadioId)

        // Auto index
        autoIndexSwitch.isChecked = SettingsManager.isAutoIndexEnabled(this)

        // Auto index time
        val time = SettingsManager.getAutoIndexTime(this)
        autoIndexTimeButton.text = String.format(getString(R.string.auto_index_time), time)

        // Speech output
        speechOutputSwitch.isChecked = SettingsManager.isSpeechOutputEnabled(this)

        // Voice gender
        val gender = SettingsManager.getVoiceGender(this)
        val voiceRadioId = when (gender) {
            "male" -> R.id.radioVoiceMale
            else -> R.id.radioVoiceFemale
        }
        voiceRadioGroup.check(voiceRadioId)
    }

    private fun showTimePicker() {
        val time = SettingsManager.getAutoIndexTime(this)
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 2
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                SettingsManager.setAutoIndexTime(this, timeString)
                autoIndexTimeButton.text = String.format(getString(R.string.auto_index_time), timeString)
                
                if (SettingsManager.isAutoIndexEnabled(this)) {
                    AutoIndexScheduler.schedule(this, selectedHour, selectedMinute)
                }
            },
            hour,
            minute,
            true
        ).show()
    }
}
