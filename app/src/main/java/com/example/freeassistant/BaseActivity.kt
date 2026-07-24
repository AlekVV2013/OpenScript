package com.example.freeassistant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar

abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val toolbar = findViewById<MaterialToolbar>(com.example.freeassistant.R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            drawerLayout.open()
        }
        
        drawerLayout = findViewById(com.example.freeassistant.R.id.drawerLayout)
    }
}
