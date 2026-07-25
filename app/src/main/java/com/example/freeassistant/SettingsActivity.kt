package com.example.freeassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

class SettingsActivity : BaseActivity() {
    private lateinit var autoIndexSwitch: SwitchMaterial
    private lateinit var autoIndexTimeButton: Button
    private lateinit var speechOutputSwitch: SwitchMaterial
    private lateinit var voiceRadioGroup: RadioGroup
    private lateinit var openWeatherApiKeyInput: EditText

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission is recommended for background indexing visibility.
        }

    private val photoPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Photo permission is required for indexing.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val radioGroup = findViewById<RadioGroup>(R.id.languageRadioGroup)
        val currentLanguage = LanguageManager.getLanguage(this)
        if (currentLanguage == "ru") {
            radioGroup.check(R.id.radioRussian)
        } else {
            radioGroup.check(R.id.radioEnglish)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = if (checkedId == R.id.radioRussian) "ru" else "en"
            if (selected != currentLanguage) {
                LanguageManager.setLanguage(this, selected)
                restartApp()
            }
        }

        autoIndexSwitch = findViewById(R.id.autoIndexSwitch)
        autoIndexTimeButton = findViewById(R.id.autoIndexTimeButton)

        val autoIndexEnabled = SettingsManager.isAutoIndexEnabled(this)
        autoIndexSwitch.isChecked = autoIndexEnabled
        autoIndexTimeButton.isEnabled = autoIndexEnabled
        updateTimeButton()

        autoIndexSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setAutoIndexEnabled(this, isChecked)
            autoIndexTimeButton.isEnabled = isChecked
            if (isChecked) {
                ensureBackgroundPermissions()
                AutoIndexScheduler.schedule(
                    this,
                    SettingsManager.getAutoIndexHour(this),
                    SettingsManager.getAutoIndexMinute(this)
                )
            } else {
                AutoIndexScheduler.cancel(this)
            }
        }

        autoIndexTimeButton.setOnClickListener {
            showTimePicker()
        }

        speechOutputSwitch = findViewById(R.id.speechOutputSwitch)
        voiceRadioGroup = findViewById(R.id.voiceRadioGroup)

        val speechEnabled = SettingsManager.isSpeechOutputEnabled(this)
        speechOutputSwitch.isChecked = speechEnabled
        setVoiceControlsEnabled(speechEnabled)

        val voice = SettingsManager.getVoice(this)
        if (voice == "male") {
            voiceRadioGroup.check(R.id.radioVoiceMale)
        } else {
            voiceRadioGroup.check(R.id.radioVoiceFemale)
        }

        speechOutputSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setSpeechOutputEnabled(this, isChecked)
            setVoiceControlsEnabled(isChecked)
        }

        voiceRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedVoice = if (checkedId == R.id.radioVoiceMale) {
                "male"
            } else {
                "female"
            }
            SettingsManager.setVoice(this, selectedVoice)
        }

        openWeatherApiKeyInput = findViewById(R.id.openWeatherApiKeyInput)
        openWeatherApiKeyInput.setText(
            SettingsManager.getOpenWeatherApiKey(this)
        )
        openWeatherApiKeyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                SettingsManager.setOpenWeatherApiKey(
                    this@SettingsActivity,
                    s?.toString().orEmpty()
                )
            }
        })
    }

    private fun setVoiceControlsEnabled(enabled: Boolean) {
        voiceRadioGroup.isEnabled = enabled
        voiceRadioGroup.alpha = if (enabled) 1.0f else 0.5f
        for (i in 0 until voiceRadioGroup.childCount) {
            voiceRadioGroup.getChildAt(i).isEnabled = enabled
        }
    }

    private fun ensureBackgroundPermissions() {
        if (!hasImagePermission()) {
            val permissions = when {
                Build.VERSION.SDK_INT >= 34 -> arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
                else -> arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            photoPermissionLauncher.launch(permissions)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, notificationPermission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(notificationPermission)
            }
        }
    }

    private fun hasImagePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 34 -> {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(SettingsManager.getAutoIndexHour(this))
            .setMinute(SettingsManager.getAutoIndexMinute(this))
            .setTitleText(getString(R.string.choose_time))
            .build()

        picker.addOnPositiveButtonClickListener {
            SettingsManager.setAutoIndexTime(
                this,
                picker.hour,
                picker.minute
            )
            updateTimeButton()
            if (SettingsManager.isAutoIndexEnabled(this)) {
                AutoIndexScheduler.schedule(this, picker.hour, picker.minute)
            }
        }

        picker.show(supportFragmentManager, "auto_index_time_picker")
    }

    private fun updateTimeButton() {
        val hour = SettingsManager.getAutoIndexHour(this)
        val minute = SettingsManager.getAutoIndexMinute(this)
        val time = String.format(Locale.US, "%02d:%02d", hour, minute)
        autoIndexTimeButton.text = getString(R.string.auto_index_time, time)
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
