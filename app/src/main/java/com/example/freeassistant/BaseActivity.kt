package com.example.freeassistant

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val config = LanguageManager.applyLanguage(newBase)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}
